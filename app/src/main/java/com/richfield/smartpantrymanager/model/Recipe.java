package com.richfield.smartpantrymanager.model;

/**
 * Represents a single recipe. Mirrors one row of the recipes table.
 * The ingredients for a recipe live in a separate table
 * (recipe_ingredients) so each one can be checked individually by the
 * matching algorithm.
 */
public class Recipe {

    private long id;
    private String name;
    private String instructions;
    private boolean favorite;

    public Recipe(long id, String name, String instructions, boolean favorite) {
        this.id = id;
        this.name = name;
        this.instructions = instructions;
        this.favorite = favorite;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
}
