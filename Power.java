package com.pouvoirs.roulette;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Liste de tous les pouvoirs. Pour en ajouter un : ajoute simplement une ligne ici.
 * - fx(...)   : effet de potion permanent
 * - attr(...) : modificateur d'attribut permanent
 * Les pouvoirs sans fx/attr (VAMPIRE, DOUBLE_SAUT...) sont geres dans PowerListener.
 */
public enum Power {

    // ======================= POUVOIRS COOL =======================
    VITESSE("Vitesse éclair", Material.SUGAR, Rarity.COOL,
            "Vitesse II en permanence : tu files comme le vent.",
            fx(PotionEffectType.SPEED, 1)),

    SUPER_RESSORTS("Super ressorts", Material.SLIME_BALL, Rarity.COOL,
            "Saut III et aucun dégât de chute.",
            fx(PotionEffectType.JUMP_BOOST, 2)),

    COLOSSE("Colosse", Material.GOLDEN_APPLE, Rarity.COOL,
            "+10 cœurs de vie : 20 cœurs au total.",
            attr(Attribute.MAX_HEALTH, 20.0, AttributeModifier.Operation.ADD_NUMBER)),

    PEAU_DE_DRAGON("Peau de dragon", Material.IRON_CHESTPLATE, Rarity.COOL,
            "+8 d'armure naturelle, même sans équipement.",
            attr(Attribute.ARMOR, 8.0, AttributeModifier.Operation.ADD_NUMBER)),

    REGENERATION("Régénération", Material.GLISTERING_MELON_SLICE, Rarity.COOL,
            "Tes blessures se referment toutes seules.",
            fx(PotionEffectType.REGENERATION, 0)),

    IGNIFUGE("Ignifugé", Material.MAGMA_CREAM, Rarity.COOL,
            "Immunisé au feu et à la lave.",
            fx(PotionEffectType.FIRE_RESISTANCE, 0)),

    AQUAMAN("Aquaman", Material.HEART_OF_THE_SEA, Rarity.COOL,
            "Tu respires sous l'eau et tu nages comme un dauphin.",
            fx(PotionEffectType.WATER_BREATHING, 0),
            fx(PotionEffectType.DOLPHINS_GRACE, 0)),

    OEIL_DE_CHAT("Œil de chat", Material.GOLDEN_CARROT, Rarity.COOL,
            "Vision nocturne permanente.",
            fx(PotionEffectType.NIGHT_VISION, 0)),

    MINEUR_NAIN("Mineur nain", Material.GOLDEN_PICKAXE, Rarity.COOL,
            "Hâte II : tu mines à toute vitesse.",
            fx(PotionEffectType.HASTE, 1)),

    GEANT("Géant", Material.BONE, Rarity.COOL,
            "Tu grandis de 50 % et ta portée augmente.",
            attr(Attribute.SCALE, 0.5, AttributeModifier.Operation.ADD_SCALAR),
            attr(Attribute.ENTITY_INTERACTION_RANGE, 1.0, AttributeModifier.Operation.ADD_NUMBER),
            attr(Attribute.BLOCK_INTERACTION_RANGE, 1.0, AttributeModifier.Operation.ADD_NUMBER)),

    GUERRIER("Force du guerrier", Material.IRON_SWORD, Rarity.COOL,
            "Force I permanente : tu frappes plus fort.",
            fx(PotionEffectType.STRENGTH, 0)),

    VAMPIRE("Vampire", Material.REDSTONE, Rarity.COOL,
            "Tu récupères 30 % des dégâts que tu infliges en points de vie."),

    DOUBLE_SAUT("Double saut", Material.FEATHER, Rarity.COOL,
            "Appuie deux fois sur saut en l'air pour rebondir."),

    FANTOME("Fantôme", Material.GHAST_TEAR, Rarity.COOL,
            "Invisible en permanence (ton armure reste visible).",
            fx(PotionEffectType.INVISIBILITY, 0)),

    ESTOMAC_D_ACIER("Estomac d'acier", Material.COOKED_BEEF, Rarity.COOL,
            "Ta barre de faim ne descend plus jamais."),

    CHANCEUX("Chanceux", Material.RABBIT_FOOT, Rarity.COOL,
            "+5 de chance : meilleurs butins et meilleure pêche.",
            attr(Attribute.LUCK, 5.0, AttributeModifier.Operation.ADD_NUMBER)),

    INCREVABLE("Increvable", Material.TNT, Rarity.COOL,
            "Tu ne subis plus aucun dégât d'explosion."),

    // ======================= POUVOIRS NULS =======================
    TORTUE("Tortue", Material.TURTLE_HELMET, Rarity.BAD,
            "Lenteur permanente. Bon courage pour arriver à l'heure.",
            fx(PotionEffectType.SLOWNESS, 0)),

    VERRE_FRAGILE("Verre fragile", Material.GLASS, Rarity.BAD,
            "-5 cœurs : tu n'as plus que 5 cœurs de vie.",
            attr(Attribute.MAX_HEALTH, -10.0, AttributeModifier.Operation.ADD_NUMBER)),

    VENTRE_SANS_FOND("Ventre sans fond", Material.ROTTEN_FLESH, Rarity.BAD,
            "Faim permanente : ta barre de faim fond à vue d'œil.",
            fx(PotionEffectType.HUNGER, 0)),

    MALADROIT("Maladroit", Material.WOODEN_PICKAXE, Rarity.BAD,
            "Fatigue de minage permanente.",
            fx(PotionEffectType.MINING_FATIGUE, 0)),

    BRAS_MOUS("Bras mous", Material.STICK, Rarity.BAD,
            "Faiblesse permanente : tu frappes comme un poussin.",
            fx(PotionEffectType.WEAKNESS, 0)),

    MAINS_DE_BEURRE("Mains de beurre", Material.HONEYCOMB, Rarity.BAD,
            "8 % de chance de lâcher ton objet quand tu frappes."),

    PEAU_DE_PAPIER("Peau de papier", Material.PAPER, Rarity.BAD,
            "Tu subis 50 % de dégâts en plus."),

    POISSE("Poisse", Material.COBWEB, Rarity.BAD,
            "-5 de chance : butins et pêche pourris.",
            attr(Attribute.LUCK, -5.0, AttributeModifier.Operation.ADD_NUMBER)),

    BRAS_COURTS("Bras courts", Material.STRING, Rarity.BAD,
            "Ta portée est réduite pour frapper et pour casser des blocs.",
            attr(Attribute.ENTITY_INTERACTION_RANGE, -1.5, AttributeModifier.Operation.ADD_NUMBER),
            attr(Attribute.BLOCK_INTERACTION_RANGE, -2.0, AttributeModifier.Operation.ADD_NUMBER)),

    AMPOULE_HUMAINE("Ampoule humaine", Material.GLOWSTONE, Rarity.BAD,
            "Tu brilles : tout le monde te voit à travers les murs.",
            fx(PotionEffectType.GLOWING, 0)),

    PYROPHOBE("Pyrophobe", Material.BLAZE_POWDER, Rarity.BAD,
            "Les dégâts de feu et de lave sont doublés."),

    NEANT("Le Néant", Material.BARRIER, Rarity.BAD,
            "Tu as tiré... rien du tout. Pas de pouvoir. Désolé !");

    // ------------------------------------------------------------------

    public enum Rarity {
        COOL("✦ Pouvoir cool", NamedTextColor.GREEN),
        BAD("✘ Pouvoir nul", NamedTextColor.RED);

        private final String label;
        private final NamedTextColor color;

        Rarity(String label, NamedTextColor color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public NamedTextColor color() {
            return color;
        }
    }

    public record Fx(PotionEffectType type, int amplifier) {
    }

    public record Attr(Attribute attribute, double amount, AttributeModifier.Operation operation) {
    }

    private final String displayName;
    private final Material icon;
    private final Rarity rarity;
    private final String description;
    private final List<Fx> effects = new ArrayList<>();
    private final List<Attr> attributes = new ArrayList<>();

    Power(String displayName, Material icon, Rarity rarity, String description, Object... specs) {
        this.displayName = displayName;
        this.icon = icon;
        this.rarity = rarity;
        this.description = description;
        for (Object spec : specs) {
            if (spec instanceof Fx fx) {
                effects.add(fx);
            } else if (spec instanceof Attr attr) {
                attributes.add(attr);
            }
        }
    }

    private static Fx fx(PotionEffectType type, int amplifier) {
        return new Fx(type, amplifier);
    }

    private static Attr attr(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        return new Attr(attribute, amount, operation);
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public Rarity rarity() {
        return rarity;
    }

    public String description() {
        return description;
    }

    public List<Fx> effects() {
        return Collections.unmodifiableList(effects);
    }

    public List<Attr> attributes() {
        return Collections.unmodifiableList(attributes);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Power fromId(String id) {
        if (id == null) {
            return null;
        }
        try {
            return Power.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
