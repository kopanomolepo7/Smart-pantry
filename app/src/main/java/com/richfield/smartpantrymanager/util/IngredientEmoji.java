package com.richfield.smartpantrymanager.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Purely cosmetic: maps a normalised ingredient name to a representative
 * emoji so pantry rows and recipe ingredient lists have a bit more
 * character than plain text. Falls back to a generic pantry emoji for
 * anything not in the map (e.g. a custom ingredient the user typed in
 * that isn't one of the seeded recipe ingredients).
 */
public final class IngredientEmoji {

    private static final Map<String, String> EMOJI_BY_INGREDIENT = new HashMap<>();
    private static final String FALLBACK_EMOJI = "\uD83E\uDD6B"; // 🥫

    static {
        EMOJI_BY_INGREDIENT.put("egg", "\uD83E\uDD5A");        // 🥚
        EMOJI_BY_INGREDIENT.put("tomato", "\uD83C\uDF45");     // 🍅
        EMOJI_BY_INGREDIENT.put("onion", "\uD83E\uDDC5");      // 🧅
        EMOJI_BY_INGREDIENT.put("garlic", "\uD83E\uDDC4");     // 🧄
        EMOJI_BY_INGREDIENT.put("cheese", "\uD83E\uDDC0");     // 🧀
        EMOJI_BY_INGREDIENT.put("butter", "\uD83E\uDDC8");     // 🧈
        EMOJI_BY_INGREDIENT.put("milk", "\uD83E\uDD5B");       // 🥛
        EMOJI_BY_INGREDIENT.put("bread", "\uD83C\uDF5E");      // 🍞
        EMOJI_BY_INGREDIENT.put("pasta", "\uD83C\uDF5D");      // 🍝
        EMOJI_BY_INGREDIENT.put("rice", "\uD83C\uDF5A");       // 🍚
        EMOJI_BY_INGREDIENT.put("chicken", "\uD83C\uDF57");    // 🍗
        EMOJI_BY_INGREDIENT.put("potato", "\uD83E\uDD54");     // 🥔
        EMOJI_BY_INGREDIENT.put("carrot", "\uD83E\uDD55");     // 🥕
        EMOJI_BY_INGREDIENT.put("spinach", "\uD83E\uDD6C");    // 🥬
        EMOJI_BY_INGREDIENT.put("lemon", "\uD83C\uDF4B");      // 🍋
        EMOJI_BY_INGREDIENT.put("flour", "\uD83C\uDF3E");      // 🌾
        EMOJI_BY_INGREDIENT.put("sugar", "\uD83E\uDDC2");      // 🧂
        EMOJI_BY_INGREDIENT.put("olive oil", "\uD83E\uDED2");  // 🫒
    }

    private IngredientEmoji() { }

    public static String forIngredient(String rawName) {
        String key = IngredientMatcher.normaliseName(rawName);
        String emoji = EMOJI_BY_INGREDIENT.get(key);
        return emoji != null ? emoji : FALLBACK_EMOJI;
    }
}
