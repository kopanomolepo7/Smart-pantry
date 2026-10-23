package com.richfield.smartpantrymanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import com.richfield.smartpantrymanager.model.PantryItem;
import com.richfield.smartpantrymanager.model.Recipe;
import com.richfield.smartpantrymanager.model.RecipeIngredient;
import com.richfield.smartpantrymanager.util.IngredientMatcher;

/**
 * Single source of truth for local persistence. Owns three tables:
 *
 *   pantry_items       - the user's own ingredients (CRUD by the user)
 *   recipes             - static recipe catalogue, seeded on first run
 *   recipe_ingredients  - one row per (recipe, required ingredient) pair,
 *                         so the matching algorithm can check each
 *                         ingredient individually instead of parsing a
 *                         free-text list.
 *
 * Database logic is kept out of the Activities/Fragments entirely so UI
 * code and persistence code can be understood and tested separately.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_pantry.db";
    private static final int DATABASE_VERSION = 2; // v2 added recipes.is_favorite

    private static final String TABLE_PANTRY = "pantry_items";
    private static final String TABLE_RECIPES = "recipes";
    private static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";

    // pantry_items columns
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_QUANTITY = "quantity";
    private static final String COL_UNIT = "unit";
    private static final String COL_EXPIRY = "expiry_date";

    // recipes columns
    private static final String COL_RECIPE_NAME = "name";
    private static final String COL_INSTRUCTIONS = "instructions";
    private static final String COL_IS_FAVORITE = "is_favorite";

    // recipe_ingredients columns
    private static final String COL_RECIPE_ID = "recipe_id";
    private static final String COL_INGREDIENT_NAME = "ingredient_name";
    private static final String COL_REQUIRED_QTY = "required_quantity";
    private static final String COL_ING_UNIT = "unit";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT NOT NULL, " +
                COL_QUANTITY + " REAL NOT NULL, " +
                COL_UNIT + " TEXT, " +
                COL_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RECIPE_NAME + " TEXT NOT NULL, " +
                COL_INSTRUCTIONS + " TEXT, " +
                COL_IS_FAVORITE + " INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RECIPE_ID + " INTEGER NOT NULL, " +
                COL_INGREDIENT_NAME + " TEXT NOT NULL, " +
                COL_REQUIRED_QTY + " REAL NOT NULL, " +
                COL_ING_UNIT + " TEXT, " +
                "FOREIGN KEY(" + COL_RECIPE_ID + ") REFERENCES " + TABLE_RECIPES + "(" + COL_ID + "))");

        seedRecipes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Additive migration only — never drop pantry_items, or every
        // user's own pantry data would be wiped on an app update.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECIPES +
                    " ADD COLUMN " + COL_IS_FAVORITE + " INTEGER NOT NULL DEFAULT 0");
        }
    }

    // ---------------------------------------------------------------
    // Pantry CRUD
    // ---------------------------------------------------------------

    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = pantryValues(item);
        return db.insert(TABLE_PANTRY, null, values);
    }

    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = pantryValues(item);
        return db.update(TABLE_PANTRY, values, COL_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
    }

    public void deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PANTRY, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public PantryItem getPantryItemById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PANTRY, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        PantryItem item = null;
        if (cursor.moveToFirst()) {
            item = pantryItemFromCursor(cursor);
        }
        cursor.close();
        return item;
    }

    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PANTRY, null, null, null, null, null, COL_NAME + " ASC");
        while (cursor.moveToNext()) {
            items.add(pantryItemFromCursor(cursor));
        }
        cursor.close();
        return items;
    }

    private ContentValues pantryValues(PantryItem item) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, item.getName());
        values.put(COL_QUANTITY, item.getQuantity());
        values.put(COL_UNIT, item.getUnit());
        values.put(COL_EXPIRY, item.getExpiryDate());
        return values;
    }

    private PantryItem pantryItemFromCursor(Cursor cursor) {
        return new PantryItem(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_QUANTITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPIRY))
        );
    }

    // ---------------------------------------------------------------
    // Recipe reads (recipes are seeded, not user-edited — only their
    // favorite flag can change) and the "Cook this recipe" action
    // ---------------------------------------------------------------

    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, null, null, null, null, COL_RECIPE_NAME + " ASC");
        while (cursor.moveToNext()) {
            recipes.add(recipeFromCursor(cursor));
        }
        cursor.close();
        return recipes;
    }

    public Recipe getRecipeById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        Recipe recipe = null;
        if (cursor.moveToFirst()) {
            recipe = recipeFromCursor(cursor);
        }
        cursor.close();
        return recipe;
    }

    private Recipe recipeFromCursor(Cursor cursor) {
        return new Recipe(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_INSTRUCTIONS)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_FAVORITE)) != 0
        );
    }

    public void setRecipeFavorite(long recipeId, boolean favorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_FAVORITE, favorite ? 1 : 0);
        db.update(TABLE_RECIPES, values, COL_ID + " = ?", new String[]{String.valueOf(recipeId)});
    }

    public List<RecipeIngredient> getIngredientsForRecipe(long recipeId) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPE_INGREDIENTS, null, COL_RECIPE_ID + " = ?",
                new String[]{String.valueOf(recipeId)}, null, null, COL_ID + " ASC");
        while (cursor.moveToNext()) {
            ingredients.add(new RecipeIngredient(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_RECIPE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_INGREDIENT_NAME)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_REQUIRED_QTY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_ING_UNIT))
            ));
        }
        cursor.close();
        return ingredients;
    }

    /**
     * "Cook this recipe": deducts every required ingredient's quantity
     * from the matching pantry item, converting units where needed. If
     * a deduction would leave zero or less, the pantry item is removed
     * entirely rather than left at a meaningless zero/negative amount.
     *
     * Re-checks the strict match right before deducting (the pantry may
     * have changed since the screen was opened) so we never deduct a
     * recipe the user no longer actually has all the ingredients for.
     * Runs as a single transaction so a failure partway through can't
     * leave the pantry half-updated.
     *
     * @return true if the deduction was applied, false if the recipe no
     *         longer matches the current pantry.
     */
    public boolean deductIngredientsForRecipe(long recipeId) {
        List<RecipeIngredient> required = getIngredientsForRecipe(recipeId);
        List<PantryItem> pantryItems = getAllPantryItems();

        if (!IngredientMatcher.recipeMatchesPantry(required, pantryItems)) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (RecipeIngredient ingredient : required) {
                PantryItem match = IngredientMatcher.findPantryMatch(ingredient.getIngredientName(), pantryItems);
                if (match == null) {
                    return false; // shouldn't happen given the check above, but don't crash
                }

                double amountToDeduct = IngredientMatcher.convertQuantity(
                        ingredient.getRequiredQuantity(), ingredient.getUnit(), match.getUnit());
                double remaining = match.getQuantity() - amountToDeduct;

                if (remaining <= 0.0001) {
                    db.delete(TABLE_PANTRY, COL_ID + " = ?", new String[]{String.valueOf(match.getId())});
                } else {
                    ContentValues values = new ContentValues();
                    values.put(COL_QUANTITY, remaining);
                    db.update(TABLE_PANTRY, values, COL_ID + " = ?", new String[]{String.valueOf(match.getId())});
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // ---------------------------------------------------------------
    // First-run recipe seeding
    // ---------------------------------------------------------------

    /**
     * Seeds 18 recipes with deliberately overlapping ingredients
     * (egg, tomato, onion, garlic, cheese, butter, rice, etc.) so the
     * strict-matching algorithm has real variety to work with: some
     * recipes will match with a small pantry, others need one more
     * ingredient, others need a larger quantity than a typical pantry
     * would have — good material for demonstrating match/no-match
     * scenarios in the video.
     */
    private void seedRecipes(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            insertRecipe(db, "Scrambled Eggs",
                    "1. Whisk eggs with milk.\n2. Melt butter in a pan.\n3. Cook eggs on low heat, stirring gently until set.",
                    new Object[][]{
                            {"egg", 2.0, "pcs"},
                            {"milk", 50.0, "ml"},
                            {"butter", 10.0, "g"}
                    });

            insertRecipe(db, "Tomato Onion Salad",
                    "1. Slice tomato and onion.\n2. Toss with olive oil.\n3. Season and serve.",
                    new Object[][]{
                            {"tomato", 2.0, "pcs"},
                            {"onion", 1.0, "pcs"},
                            {"olive oil", 15.0, "ml"}
                    });

            insertRecipe(db, "Cheese Omelette",
                    "1. Whisk eggs.\n2. Pour into a hot buttered pan.\n3. Add cheese, fold and serve.",
                    new Object[][]{
                            {"egg", 2.0, "pcs"},
                            {"cheese", 30.0, "g"},
                            {"butter", 10.0, "g"}
                    });

            insertRecipe(db, "Grilled Cheese Sandwich",
                    "1. Butter bread slices.\n2. Add cheese between slices.\n3. Grill until golden on both sides.",
                    new Object[][]{
                            {"bread", 2.0, "pcs"},
                            {"cheese", 50.0, "g"},
                            {"butter", 10.0, "g"}
                    });

            insertRecipe(db, "Garlic Butter Pasta",
                    "1. Boil pasta until al dente.\n2. Melt butter with crushed garlic.\n3. Toss pasta through the garlic butter.",
                    new Object[][]{
                            {"pasta", 200.0, "g"},
                            {"garlic", 2.0, "pcs"},
                            {"butter", 20.0, "g"}
                    });

            insertRecipe(db, "Tomato Pasta",
                    "1. Boil pasta.\n2. Fry garlic in olive oil, add chopped tomato and simmer.\n3. Toss pasta through the sauce.",
                    new Object[][]{
                            {"pasta", 200.0, "g"},
                            {"tomato", 3.0, "pcs"},
                            {"garlic", 1.0, "pcs"},
                            {"olive oil", 15.0, "ml"}
                    });

            insertRecipe(db, "Chicken Rice Bowl",
                    "1. Cook rice.\n2. Pan-fry chicken with sliced onion.\n3. Serve chicken and onion over rice.",
                    new Object[][]{
                            {"chicken", 300.0, "g"},
                            {"rice", 200.0, "g"},
                            {"onion", 1.0, "pcs"}
                    });

            insertRecipe(db, "Vegetable Fried Rice",
                    "1. Cook rice and cool.\n2. Stir-fry carrot and onion.\n3. Add rice and egg, stir-fry until combined.",
                    new Object[][]{
                            {"rice", 250.0, "g"},
                            {"carrot", 1.0, "pcs"},
                            {"onion", 1.0, "pcs"},
                            {"egg", 1.0, "pcs"}
                    });

            insertRecipe(db, "Potato Salad",
                    "1. Boil potato until tender, cool and cube.\n2. Mix with chopped onion and olive oil.",
                    new Object[][]{
                            {"potato", 3.0, "pcs"},
                            {"onion", 1.0, "pcs"},
                            {"olive oil", 15.0, "ml"}
                    });

            insertRecipe(db, "Mashed Potatoes",
                    "1. Boil potato until soft.\n2. Mash with butter and milk until smooth.",
                    new Object[][]{
                            {"potato", 4.0, "pcs"},
                            {"butter", 20.0, "g"},
                            {"milk", 50.0, "ml"}
                    });

            insertRecipe(db, "Carrot Soup",
                    "1. Simmer carrot and onion until soft.\n2. Blend with milk until smooth.\n3. Reheat gently and serve.",
                    new Object[][]{
                            {"carrot", 3.0, "pcs"},
                            {"onion", 1.0, "pcs"},
                            {"milk", 100.0, "ml"}
                    });

            insertRecipe(db, "Spinach Salad",
                    "1. Toss spinach with lemon juice and olive oil.\n2. Serve immediately.",
                    new Object[][]{
                            {"spinach", 100.0, "g"},
                            {"lemon", 1.0, "pcs"},
                            {"olive oil", 15.0, "ml"}
                    });

            insertRecipe(db, "Chicken Stir Fry",
                    "1. Slice chicken and stir-fry until browned.\n2. Add carrot, onion and garlic, stir-fry until tender.",
                    new Object[][]{
                            {"chicken", 250.0, "g"},
                            {"carrot", 1.0, "pcs"},
                            {"onion", 1.0, "pcs"},
                            {"garlic", 2.0, "pcs"}
                    });

            insertRecipe(db, "Egg Fried Rice",
                    "1. Cook rice and cool.\n2. Scramble egg in a hot pan, add onion and rice, stir-fry until combined.",
                    new Object[][]{
                            {"rice", 200.0, "g"},
                            {"egg", 2.0, "pcs"},
                            {"onion", 1.0, "pcs"}
                    });

            insertRecipe(db, "Buttered Toast",
                    "1. Toast bread.\n2. Spread with butter while warm.",
                    new Object[][]{
                            {"bread", 2.0, "pcs"},
                            {"butter", 15.0, "g"}
                    });

            insertRecipe(db, "Pancakes",
                    "1. Whisk flour, milk and egg into a batter.\n2. Stir in sugar.\n3. Cook spoonfuls on a hot pan until golden on both sides.",
                    new Object[][]{
                            {"flour", 150.0, "g"},
                            {"milk", 100.0, "ml"},
                            {"egg", 1.0, "pcs"},
                            {"sugar", 20.0, "g"}
                    });

            insertRecipe(db, "Lemon Rice",
                    "1. Cook rice.\n2. Fry garlic briefly, stir through rice with lemon juice.",
                    new Object[][]{
                            {"rice", 200.0, "g"},
                            {"lemon", 1.0, "pcs"},
                            {"garlic", 1.0, "pcs"}
                    });

            insertRecipe(db, "Cheesy Garlic Bread",
                    "1. Mix softened butter with crushed garlic and spread on bread.\n2. Top with cheese.\n3. Grill until melted and golden.",
                    new Object[][]{
                            {"bread", 2.0, "pcs"},
                            {"cheese", 40.0, "g"},
                            {"garlic", 2.0, "pcs"},
                            {"butter", 15.0, "g"}
                    });

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertRecipe(SQLiteDatabase db, String name, String instructions, Object[][] ingredients) {
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(COL_RECIPE_NAME, name);
        recipeValues.put(COL_INSTRUCTIONS, instructions);
        recipeValues.put(COL_IS_FAVORITE, 0);
        long recipeId = db.insert(TABLE_RECIPES, null, recipeValues);

        for (Object[] ingredient : ingredients) {
            ContentValues ingredientValues = new ContentValues();
            ingredientValues.put(COL_RECIPE_ID, recipeId);
            ingredientValues.put(COL_INGREDIENT_NAME, (String) ingredient[0]);
            ingredientValues.put(COL_REQUIRED_QTY, (Double) ingredient[1]);
            ingredientValues.put(COL_ING_UNIT, (String) ingredient[2]);
            db.insert(TABLE_RECIPE_INGREDIENTS, null, ingredientValues);
        }
    }
}
