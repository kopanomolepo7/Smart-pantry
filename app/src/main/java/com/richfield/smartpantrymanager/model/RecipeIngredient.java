package com.richfield.smartpantrymanager.model;

/**
 * A single required ingredient line for a recipe (one row of the
 * recipe_ingredients table). A recipe has many of these — this is what
 * lets the matching algorithm check "every required ingredient" one at
 * a time instead of parsing a free-text ingredient list.
 */
public class RecipeIngredient {

    private long id;
    private long recipeId;
    private String ingredientName;
    private double requiredQuantity;
    private String unit;

    public RecipeIngredient(long id, long recipeId, String ingredientName,
                             double requiredQuantity, String unit) {
        this.id = id;
        this.recipeId = recipeId;
        this.ingredientName = ingredientName;
        this.requiredQuantity = requiredQuantity;
        this.unit = unit;
    }

    public long getId() { return id; }
    public long getRecipeId() { return recipeId; }
    public String getIngredientName() { return ingredientName; }
    public double getRequiredQuantity() { return requiredQuantity; }
    public String getUnit() { return unit; }
}
