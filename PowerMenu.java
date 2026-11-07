package com.pouvoirs.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Menu d'intro (liste des pouvoirs + bouton START) et animation de roulette.
 */
public final class PowerMenu implements Listener {

    private static final int INTRO_SIZE = 54;
    private static final int START_SLOT = 49;
    private static final int SPIN_SIZE = 27;
    private static final int FIRST_SLOT = 9;   // la roulette occupe les slots 9 a 17
    private static final int CENTER_SLOT = 13; // le pouvoir gagnant s'arrete ici
    private static final int TOTAL_FRAMES = 50;

    private final PouvoirsPlugin plugin;
    private final PowerManager manager;
    private final Map<UUID, SpinState> spins = new HashMap<>();

    public PowerMenu(PouvoirsPlugin plugin, PowerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ------------------------------------------------------------ holders

    private static final class IntroHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class SpinHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class SpinState {
        private final UUID id;
        private final Inventory inventory;
        private final Power[] sequence;
        private final Power winner;
        private int frame = 0;

        private SpinState(UUID id, Inventory inventory, Power[] sequence, Power winner) {
            this.id = id;
            this.inventory = inventory;
            this.sequence = sequence;
            this.winner = winner;
        }
    }

    // ------------------------------------------------------------ menu d'intro

    public void openIntro(Player player) {
        if (spins.containsKey(player.getUniqueId()) || manager.hasPower(player)) {
            return;
        }

        IntroHolder holder = new IntroHolder();
        Inventory inventory = Bukkit.createInventory(holder, INTRO_SIZE,
                Component.text("Choisis ton destin", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.inventory = inventory;

        Power[] all = Power.values();
        for (int i = 0; i < all.length && i < 45; i++) {
            inventory.setItem(i, powerItem(all[i], true));
        }

        for (int slot = 45; slot < INTRO_SIZE; slot++) {
            inventory.setItem(slot, pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        inventory.setItem(START_SLOT, startButton());

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.2f);
    }

    private ItemStack startButton() {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain("▶ START", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(plain("Clique pour lancer la roulette !", NamedTextColor.YELLOW));
        lore.add(plain("Ton pouvoir est tiré au hasard : cool ou nul...", NamedTextColor.GRAY));
        lore.add(plain("Un seul tirage, pas de retour en arrière !", NamedTextColor.RED));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------ roulette

    private void startSpin(Player player) {
        UUID id = player.getUniqueId();
        if (spins.containsKey(id)) {
            return;
        }
        if (manager.hasPower(player)) {
            player.closeInventory();
            return;
        }

        Power winner = pickWinner();
        Power[] all = Power.values();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Power[] sequence = new Power[TOTAL_FRAMES + 9];
        for (int i = 0; i < sequence.length; i++) {
            sequence[i] = all[random.nextInt(all.length)];
        }
        // A la derniere image, le slot central (4e de la rangee) affiche le gagnant.
        sequence[TOTAL_FRAMES + 4] = winner;

        SpinHolder holder = new SpinHolder();
        Inventory inventory = Bukkit.createInventory(holder, SPIN_SIZE,
                Component.text("La roulette tourne...", NamedTextColor.GOLD, TextDecoration.BOLD));
        holder.inventory = inventory;

        for (int slot = 0; slot < SPIN_SIZE; slot++) {
            inventory.setItem(slot, pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        inventory.setItem(4, pane(Material.LIME_STAINED_GLASS_PANE, "▼ Ton pouvoir ▼"));
        inventory.setItem(22, pane(Material.LIME_STAINED_GLASS_PANE, "▲ Ton pouvoir ▲"));

        SpinState state = new SpinState(id, inventory, sequence, winner);
        spins.put(id, state); // avant d'ouvrir : l'ouverture ferme le menu d'intro
        render(state);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || spins.get(id) != state) {
                return;
            }
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> advance(state), 12L);
        });
    }

    private Power pickWinner() {
        double chanceCool = plugin.getConfig().getDouble("chance-cool", 0.6);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Power.Rarity wanted = random.nextDouble() < chanceCool ? Power.Rarity.COOL : Power.Rarity.BAD;

        List<Power> pool = new ArrayList<>();
        for (Power power : Power.values()) {
            if (power.rarity() == wanted) {
                pool.add(power);
            }
        }
        if (pool.isEmpty()) {
            pool = Arrays.asList(Power.values());
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private void render(SpinState state) {
        for (int i = 0; i < 9; i++) {
            state.inventory.setItem(FIRST_SLOT + i, powerItem(state.sequence[state.frame + i], false));
        }
    }

    private void advance(SpinState state) {
        Player player = Bukkit.getPlayer(state.id);
        if (player == null || !player.isOnline() || spins.get(state.id) != state) {
            return;
        }

        if (state.frame >= TOTAL_FRAMES) {
            finish(player, state);
            return;
        }

        state.frame++;
        render(state);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f + state.frame * 0.02f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> advance(state), delayFor(state.frame));
    }

    /** La roulette ralentit petit a petit. */
    private long delayFor(int frame) {
        if (frame < 25) {
            return 1L;
        }
        if (frame < 38) {
            return 2L;
        }
        if (frame < 45) {
            return 4L;
        }
        if (frame < 48) {
            return 7L;
        }
        return 12L;
    }

    private void finish(Player player, SpinState state) {
        Power winner = state.winner;

        ItemStack center = powerItem(winner, false);
        ItemMeta meta = center.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        center.setItemMeta(meta);
        state.inventory.setItem(CENTER_SLOT, center);

        spins.remove(state.id);
        manager.setPower(player, winner);

        boolean cool = winner.rarity() == Power.Rarity.COOL;
        NamedTextColor color = winner.rarity().color();
        Location location = player.getLocation();

        player.showTitle(Title.title(
                Component.text(winner.displayName(), color, TextDecoration.BOLD),
                Component.text(winner.rarity().label(), color)));
        player.sendMessage(Component.text("Ton pouvoir : ", NamedTextColor.GOLD)
                .append(Component.text(winner.displayName(), color, TextDecoration.BOLD)));
        player.sendMessage(Component.text(winner.description(), NamedTextColor.GRAY));

        if (cool) {
            player.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    location.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.3);
        } else {
            player.playSound(location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            player.getWorld().spawnParticle(Particle.SMOKE,
                    location.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
        }

        if (plugin.getConfig().getBoolean("broadcast", true)) {
            Bukkit.broadcast(Component.text(player.getName(), NamedTextColor.YELLOW)
                    .append(Component.text(" a tiré le pouvoir ", NamedTextColor.GRAY))
                    .append(Component.text(winner.displayName(), color, TextDecoration.BOLD))
                    .append(Component.text(" !", NamedTextColor.GRAY)));
        }

        // On laisse 3 secondes pour admirer le resultat, puis on ferme.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()
                    && player.getOpenInventory().getTopInventory().getHolder(false) instanceof SpinHolder) {
                player.closeInventory();
            }
        }, 60L);
    }

    /** Ferme tous les menus ouverts (arret / rechargement du plugin). */
    public void shutdown() {
        spins.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder(false);
            if (holder instanceof IntroHolder || holder instanceof SpinHolder) {
                player.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------ evenements

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder(false);
        if (!(holder instanceof IntroHolder) && !(holder instanceof SpinHolder)) {
            return;
        }
        event.setCancelled(true); // rien ne peut etre pris, deplace ou echange dans ces menus

        if (!(holder instanceof IntroHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != top || event.getSlot() != START_SLOT) {
            return;
        }
        startSpin(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (holder instanceof IntroHolder || holder instanceof SpinHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder(false) instanceof IntroHolder)) {
            return;
        }
        if (spins.containsKey(player.getUniqueId())) {
            return; // on passe simplement du menu d'intro a la roulette
        }
        if (manager.hasPower(player) || !manager.isPending(player)) {
            return;
        }
        player.sendMessage(Component.text("Tu as fermé la roulette. ", NamedTextColor.GRAY)
                .append(Component.text("[Clique ici pour la rouvrir]", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/pouvoir"))));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spins.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------ items

    private ItemStack powerItem(Power power, boolean detailed) {
        ItemStack item = new ItemStack(power.icon());
        ItemMeta meta = item.getItemMeta();
        NamedTextColor color = power.rarity().color();
        meta.displayName(plain(power.displayName(), color).decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(plain(power.rarity().label(), color));
        if (detailed) {
            lore.add(plain(power.description(), NamedTextColor.GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name, NamedTextColor.WHITE));
        item.setItemMeta(meta);
        return item;
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
