package com.pouvoirs.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Connexion du joueur + comportement des pouvoirs qui ne passent pas
 * par un simple effet de potion ou un attribut.
 */
public final class PowerListener implements Listener {

    private final PouvoirsPlugin plugin;
    private final PowerManager manager;
    private final PowerMenu menu;

    public PowerListener(PouvoirsPlugin plugin, PowerManager manager, PowerMenu menu) {
        this.plugin = plugin;
        this.manager = manager;
        this.menu = menu;
    }

    // ------------------------------------------------------------ connexion

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        manager.load(player);
        if (manager.hasPower(player)) {
            return;
        }

        boolean firstJoinOnly = plugin.getConfig().getBoolean("first-join-only", true);
        if (!player.hasPlayedBefore() || !firstJoinOnly) {
            manager.setPending(player, true);
        }

        if (manager.isPending(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !manager.hasPower(player)) {
                    menu.openIntro(player);
                }
            }, 30L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.unload(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        refreshLater(event.getPlayer());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.MILK_BUCKET) {
            refreshLater(event.getPlayer());
        }
    }

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            refreshLater(player);
        }
    }

    private void refreshLater(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                manager.refresh(player);
            }
        }, 2L);
    }

    // ------------------------------------------------------------ degats subis

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Power power = manager.getPower(player);
        if (power == null) {
            return;
        }
        DamageCause cause = event.getCause();

        switch (power) {
            case SUPER_RESSORTS -> {
                if (cause == DamageCause.FALL) {
                    event.setCancelled(true);
                }
            }
            case INCREVABLE -> {
                if (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION) {
                    event.setCancelled(true);
                }
            }
            case PEAU_DE_PAPIER -> event.setDamage(event.getDamage() * 1.5);
            case PYROPHOBE -> {
                if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK
                        || cause == DamageCause.LAVA || cause == DamageCause.HOT_FLOOR) {
                    event.setDamage(event.getDamage() * 2.0);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------ degats infliges

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Player attacker = null;
        boolean melee = false;
        if (event.getDamager() instanceof Player direct) {
            attacker = direct;
            melee = true;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null) {
            return;
        }

        Power power = manager.getPower(attacker);
        if (power == Power.VAMPIRE) {
            if (event.getEntity() != attacker) {
                double healed = event.getFinalDamage() * 0.30;
                if (healed > 0) {
                    attacker.heal(healed);
                }
            }
        } else if (power == Power.MAINS_DE_BEURRE && melee
                && ThreadLocalRandom.current().nextDouble() < 0.08) {
            dropHeldItem(attacker);
        }
    }

    private void dropHeldItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return;
        }
        ItemStack copy = hand.clone();
        player.getInventory().setItemInMainHand(null);
        Item dropped = player.getWorld().dropItem(player.getEyeLocation(), copy);
        dropped.setPickupDelay(40);
        dropped.setVelocity(player.getLocation().getDirection().multiply(0.2));
        player.sendActionBar(Component.text("Tes mains glissent... tu as lâché ton objet !", NamedTextColor.RED));
    }

    // ------------------------------------------------------------ faim

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (manager.getPower(player) == Power.ESTOMAC_D_ACIER
                && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------ double saut

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (manager.getPower(player) != Power.DOUBLE_SAUT
                || !PowerManager.isSurvivalLike(player)
                || !event.isFlying()) {
            return;
        }

        event.setCancelled(true);
        player.setAllowFlight(false); // reactive des qu'il retouche le sol
        player.setFlying(false);

        Vector push = player.getLocation().getDirection();
        push.setY(0);
        if (push.lengthSquared() > 1.0E-4) {
            push.normalize().multiply(0.45);
        } else {
            push = new Vector(0, 0, 0);
        }
        push.setY(0.85);
        player.setVelocity(push);
        player.setFallDistance(0f);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.25, 0.05, 0.25, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.6f, 1.4f);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getAllowFlight()) {
            return;
        }
        if (manager.getPower(player) != Power.DOUBLE_SAUT
                || !PowerManager.isSurvivalLike(player)
                || !player.isOnGround()) {
            return;
        }
        player.setAllowFlight(true);
    }
}
