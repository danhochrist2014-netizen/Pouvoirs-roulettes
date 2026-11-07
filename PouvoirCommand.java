package com.pouvoirs.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /pouvoir                      : voir son pouvoir (ou rouvrir la roulette si on ne l'a pas encore tiree)
 * /pouvoir liste                : (admin) liste des identifiants de pouvoirs
 * /pouvoir reset <joueur>       : (admin) efface le pouvoir et redonne un tirage
 * /pouvoir set <joueur> <id>    : (admin) donne un pouvoir precis (pratique pour tester)
 */
public final class PouvoirCommand implements TabExecutor {

    private static final String ADMIN_PERMISSION = "pouvoirs.admin";

    private final PowerManager manager;
    private final PowerMenu menu;

    public PouvoirCommand(PouvoirsPlugin plugin, PowerManager manager, PowerMenu menu) {
        this.manager = manager;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return showOwnPower(sender);
        }

        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(Component.text("Tu n'as pas la permission de faire ça.", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "liste" -> {
                List<String> ids = new ArrayList<>();
                for (Power power : Power.values()) {
                    ids.add(power.id());
                }
                sender.sendMessage(Component.text("Pouvoirs : ", NamedTextColor.GOLD)
                        .append(Component.text(String.join(", ", ids), NamedTextColor.YELLOW)));
            }
            case "reset" -> {
                Player target = target(sender, args);
                if (target == null) {
                    return true;
                }
                manager.reset(target);
                menu.openIntro(target);
                sender.sendMessage(Component.text("Pouvoir de " + target.getName()
                        + " effacé : il peut retirer à la roulette.", NamedTextColor.GREEN));
            }
            case "set" -> {
                Player target = target(sender, args);
                if (target == null) {
                    return true;
                }
                Power power = args.length >= 3 ? Power.fromId(args[2]) : null;
                if (power == null) {
                    sender.sendMessage(Component.text("Pouvoir inconnu. Utilise /pouvoir liste.", NamedTextColor.RED));
                    return true;
                }
                manager.setPower(target, power);
                sender.sendMessage(Component.text(target.getName() + " a maintenant le pouvoir "
                        + power.displayName() + ".", NamedTextColor.GREEN));
                target.sendMessage(Component.text("Ton pouvoir est maintenant : ", NamedTextColor.GOLD)
                        .append(Component.text(power.displayName(), power.rarity().color(), TextDecoration.BOLD)));
            }
            default -> sender.sendMessage(Component.text(
                    "Usage : /pouvoir [liste | reset <joueur> | set <joueur> <pouvoir>]", NamedTextColor.RED));
        }
        return true;
    }

    private boolean showOwnPower(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        Power power = manager.getPower(player);
        if (power != null) {
            player.sendMessage(Component.text("Ton pouvoir : ", NamedTextColor.GOLD)
                    .append(Component.text(power.displayName(), power.rarity().color(), TextDecoration.BOLD)));
            player.sendMessage(Component.text(power.description(), NamedTextColor.GRAY));
            return true;
        }

        if (manager.isPending(player)) {
            menu.openIntro(player);
            return true;
        }

        player.sendMessage(Component.text(
                "Tu n'as pas de pouvoir : la roulette n'est disponible qu'à la première connexion.",
                NamedTextColor.GRAY));
        return true;
    }

    private Player target(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Précise le nom d'un joueur en ligne.", NamedTextColor.RED));
            return null;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable (il doit être en ligne).", NamedTextColor.RED));
        }
        return target;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return options;
        }

        if (args.length == 1) {
            options.add("liste");
            options.add("reset");
            options.add("set");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("set"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (Power power : Power.values()) {
                options.add(power.id());
            }
        }

        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(typed));
        return options;
    }
}
