package com.richfield.smartpantrymanager.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.richfield.smartpantrymanager.model.PantryItem;
import com.richfield.smartpantrymanager.model.RecipeIngredient;

/**
 * Implements the assignment's core business rule: a recipe is only a
 * valid suggestion if EVERY required ingredient is present in the
 * pantry at AT LEAST the required quantity.
 *
 * No partial matches, no "close enough" — a single missing ingredient
 * or insufficient quantity disqualifies the whole recipe from the main
 * Suggested Recipes list. The same per-ingredient check also powers the
 * optional "Almost There" list (recipes failing exactly one check) and
 * the "Cook this recipe" quantity deduction.
 *
 * To keep this reliable and explainable (per the assignment: no need
 * for full NLP), ingredient names are normalised with a simple
 * lowercase/trim/singularise step, and quantities are compared after
 * converting to a common base unit within the same unit "family"
 * (mass: g/kg, volume: ml/l, count: pcs).
 */
public final class IngredientMatcher {

    private IngredientMatcher() { }

    /**
     * Returns true only if every ingredient the recipe requires is
     * present in the pantry in sufficient quantity.
     */
    public static boolean recipeMatchesPantry(List<RecipeIngredient> requiredIngredients,
                                               List<PantryItem> pantryItems) {
        return countUnmetRequirements(requiredIngredients, pantryItems) == 0;
    }

    /**
     * Counts how many of the recipe's required ingredients are either
     * missing from the pantry or present in insufficient quantity.
     * 0 means a full strict match. Used both for the main Suggested
     * Recipes filter (unmet == 0) and the optional Almost There list
     * (unmet == 1).
     */
    public static int countUnmetRequirements(List<RecipeIngredient> requiredIngredients,
                                              List<PantryItem> pantryItems) {
        if (requiredIngredients == null || requiredIngredients.isEmpty()) {
            return Integer.MAX_VALUE; // a recipe with no defined ingredients can't be "matched"
        }
        int unmet = 0;
        for (RecipeIngredient required : requiredIngredients) {
            if (!isRequirementMet(required, pantryItems)) {
                unmet++;
            }
        }
        return unmet;
    }

    /**
     * Returns the display names of the ingredients that are currently
     * missing or insufficient for this recipe, in the order they're
     * defined on the recipe. Used to power the "Just need: cheese"
     * style subtitle on Almost There recipes.
     */
    public static List<String> unmetIngredientNames(List<RecipeIngredient> requiredIngredients,
                                                      List<PantryItem> pantryItems) {
        List<String> unmetNames = new ArrayList<>();
        if (requiredIngredients == null) {
            return unmetNames;
        }
        for (RecipeIngredient required : requiredIngredients) {
            if (!isRequirementMet(required, pantryItems)) {
                unmetNames.add(required.getIngredientName());
            }
        }
        return unmetNames;
    }

    private static boolean isRequirementMet(RecipeIngredient required, List<PantryItem> pantryItems) {
        PantryItem pantryMatch = findPantryMatch(required.getIngredientName(), pantryItems);
        if (pantryMatch == null) {
            return false;
        }
        String requiredFamily = unitFamily(required.getUnit());
        String pantryFamily = unitFamily(pantryMatch.getUnit());
        if (!requiredFamily.equals(pantryFamily)) {
            return false;
        }
        double requiredBaseQty = toBaseUnitQuantity(required.getRequiredQuantity(), required.getUnit());
        double pantryBaseQty = toBaseUnitQuantity(pantryMatch.getQuantity(), pantryMatch.getUnit());
        return pantryBaseQty >= requiredBaseQty;
    }

    /**
     * Finds the pantry item whose normalised name matches the given
     * required ingredient name, or null if nothing matches. Exposed
     * (not private) so DatabaseHelper can reuse it when deducting
     * quantities for "Cook this recipe".
     */
    public static PantryItem findPantryMatch(String requiredName, List<PantryItem> pantryItems) {
        String normalisedRequired = normaliseName(requiredName);
        for (PantryItem item : pantryItems) {
            if (normaliseName(item.getName()).equals(normalisedRequired)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Normalises an ingredient name so that "Tomato", "tomato ",
     * "TOMATOES" and "tomatoes" are all treated as the same ingredient:
     * lowercase, trim, collapse whitespace, strip punctuation, and a
     * simple plural-to-singular reduction (not full NLP — just enough
     * to handle common English plurals reliably).
     */
    public static String normaliseName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String name = rawName.toLowerCase(Locale.ROOT).trim();
        name = name.replaceAll("[^a-z0-9\\s]", ""); // strip punctuation
        name = name.replaceAll("\\s+", " ");        // collapse extra spaces

        if (name.endsWith("ies") && name.length() > 4) {
            name = name.substring(0, name.length() - 3) + "y"; // "berries" -> "berry"
        } else if (name.endsWith("es") && name.length() > 4) {
            name = name.substring(0, name.length() - 2);       // "tomatoes" -> "tomato"
        } else if (name.endsWith("s") && !name.endsWith("ss") && name.length() > 3) {
            name = name.substring(0, name.length() - 1);       // "eggs" -> "egg"
        }
        return name;
    }

    /**
     * Groups units into comparable families so we don't compare grams
     * against millilitres. Anything unrecognised (including null/blank,
     * which covers whole/countable items like "eggs" or "onions") is
     * treated as a simple count ("pcs").
     */
    private static String unitFamily(String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        switch (u) {
            case "g":
            case "kg":
                return "mass";
            case "ml":
            case "l":
                return "volume";
            default:
                return "count";
        }
    }

    /**
     * Converts a quantity to a common base unit within its family so
     * that, e.g., 0.5 kg and 500 g compare equal.
     * Base units: grams for mass, millilitres for volume, itself for count.
     */
    private static double toBaseUnitQuantity(double quantity, String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        switch (u) {
            case "kg":
                return quantity * 1000.0; // -> grams
            case "l":
                return quantity * 1000.0; // -> millilitres
            default:
                return quantity; // g, ml, pcs, or unrecognised: use as-is
        }
    }

    /** Inverse of toBaseUnitQuantity — converts a base-unit quantity back into a target unit. */
    private static double fromBaseUnitQuantity(double baseQuantity, String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        switch (u) {
            case "kg":
                return baseQuantity / 1000.0;
            case "l":
                return baseQuantity / 1000.0;
            default:
                return baseQuantity;
        }
    }

    /**
     * Converts a quantity from one unit to another within the same
     * family (e.g. required "500 g" expressed in the pantry item's
     * unit, which might be "kg"). Callers must only use this on units
     * already confirmed to be in the same family (isRequirementMet
     * guarantees this before a match is used for deduction).
     */
    public static double convertQuantity(double quantity, String fromUnit, String toUnit) {
        double base = toBaseUnitQuantity(quantity, fromUnit);
        return fromBaseUnitQuantity(base, toUnit);
    }
}
