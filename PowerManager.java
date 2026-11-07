package com.pouvoirs.roulette;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stocke le pouvoir de chaque joueur (dans ses donnees persistantes, donc il survit
 * aux redemarrages) et applique les effets / attributs de facon fiable.
 */
public final class PowerManager {

    private final PouvoirsPlugin plugin;
    private final NamespacedKey powerKey;
    private final NamespacedKey pendingKey;
    private final Map<UUID, Power> cache = new HashMap<>();

    public PowerManager(PouvoirsPlugin plugin) {
        this.plugin = plugin;
        this.powerKey = new NamespacedKey(plugin, "pouvoir");
        this.pendingKey = new NamespacedKey(plugin, "attente");
    }

    // ------------------------------------------------------------ etat

    /** Lit le pouvoir sauvegarde du joueur et l'applique. */
    public void load(Player player) {
        String id = player.getPersistentDataContainer().get(powerKey, PersistentDataType.STRING);
        Power power = Power.fromId(id);
        if (power == null) {
            cache.remove(player.getUniqueId());
        } else {
            cache.put(player.getUniqueId(), power);
            refresh(player);
        }
    }

    public void unload(Player player) {
        cache.remove(player.getUniqueId());
    }

    public Power getPower(Player player) {
        return cache.get(player.getUniqueId());
    }

    public boolean hasPower(Player player) {
        return cache.containsKey(player.getUniqueId());
    }

    /** "En attente" = le joueur a le droit de tirer son pouvoir mais ne l'a pas encore fait. */
    public boolean isPending(Player player) {
        Byte value = player.getPersistentDataContainer().get(pendingKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public void setPending(Player player, boolean pending) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (pending) {
            pdc.set(pendingKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(pendingKey);
        }
    }

    // ------------------------------------------------------------ actions

    /** Donne (et sauvegarde) un pouvoir au joueur, en remplacant l'ancien s'il y en avait un. */
    public void setPower(Player player, Power power) {
        clear(player);
        player.getPersistentDataContainer().set(powerKey, PersistentDataType.STRING, power.name());
        setPending(player, false);
        cache.put(player.getUniqueId(), power);
        refresh(player);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
    }

    /** Efface completement le pouvoir du joueur et le remet "en attente" d'un tirage. */
    public void reset(Player player) {
        clear(player);
        player.getPersistentDataContainer().remove(powerKey);
        setPending(player, true);
    }

    /** Retire les effets et attributs du pouvoir actuel (sans toucher a la sauvegarde). */
    public void clear(Player player) {
        Power old = cache.remove(player.getUniqueId());

        for (Power power : Power.values()) {
            List<Power.Attr> attrs = power.attributes();
            for (int i = 0; i < attrs.size(); i++) {
                AttributeInstance instance = player.getAttribute(attrs.get(i).attribute());
                if (instance == null) {
                    continue;
                }
                AttributeModifier modifier = findModifier(instance, modifierKey(power, i));
                if (modifier != null) {
                    instance.removeModifier(modifier);
                }
            }
        }

        if (old != null) {
            for (Power.Fx fx : old.effects()) {
                PotionEffect current = player.getPotionEffect(fx.type());
                if (current != null && current.isInfinite()) {
                    player.removePotionEffect(fx.type());
                }
            }
            if (old == Power.DOUBLE_SAUT && isSurvivalLike(player)) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    /**
     * Remet en place tout ce qui manque (effet efface par du lait, un totem, la mort...).
     * Appelee regulierement et apres les evenements importants.
     */
    public void refresh(Player player) {
        Power power = getPower(player);
        if (power == null) {
            return;
        }

        List<Power.Attr> attrs = power.attributes();
        for (int i = 0; i < attrs.size(); i++) {
            Power.Attr attr = attrs.get(i);
            AttributeInstance instance = player.getAttribute(attr.attribute());
            if (instance == null) {
                continue;
            }
            NamespacedKey key = modifierKey(power, i);
            if (findModifier(instance, key) == null) {
                instance.addModifier(new AttributeModifier(key, attr.amount(), attr.operation()));
            }
        }

        for (Power.Fx fx : power.effects()) {
            if (player.getPotionEffect(fx.type()) == null) {
                player.addPotionEffect(new PotionEffect(
                        fx.type(), PotionEffect.INFINITE_DURATION, fx.amplifier(), true, false, true));
            }
        }

        if (power == Power.DOUBLE_SAUT && isSurvivalLike(player)
                && !player.getAllowFlight() && player.isOnGround()) {
            player.setAllowFlight(true);
        }
    }

    public void startRefreshTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                refresh(player);
            }
        }, 40L, 40L);
    }

    // ------------------------------------------------------------ utilitaires

    public static boolean isSurvivalLike(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    private NamespacedKey modifierKey(Power power, int index) {
        return new NamespacedKey(plugin, "pouvoir_" + power.name().toLowerCase(Locale.ROOT) + "_" + index);
    }

    private AttributeModifier findModifier(AttributeInstance instance, NamespacedKey key) {
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getKey().equals(key)) {
                return modifier;
            }
        }
        return null;
    }
}
