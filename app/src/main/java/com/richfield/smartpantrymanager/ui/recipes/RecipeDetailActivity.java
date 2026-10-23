package com.richfield.smartpantrymanager.ui.recipes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;
import java.util.Locale;

import com.richfield.smartpantrymanager.R;
import com.richfield.smartpantrymanager.database.DatabaseHelper;
import com.richfield.smartpantrymanager.model.Recipe;
import com.richfield.smartpantrymanager.model.RecipeIngredient;
import com.richfield.smartpantrymanager.util.IngredientEmoji;

/**
 * Recipe Detail screen. Receives only the recipe's database id via the
 * launching Intent (not the whole object) and looks up the full recipe
 * and its ingredients itself.
 *
 * Also offers two extra actions beyond just viewing:
 *  - toggling the recipe as a favorite (persisted on the recipe row)
 *  - "Cook this recipe", which deducts the used quantities from the
 *    pantry via DatabaseHelper.deductIngredientsForRecipe
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private DatabaseHelper dbHelper;
    private Recipe recipe;
    private ImageButton favoriteButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1);
        dbHelper = new DatabaseHelper(this);
        recipe = dbHelper.getRecipeById(recipeId);

        if (recipe == null) {
            finish(); // stale id — nothing sensible to show
            return;
        }

        setTitle(recipe.getName());

        TextView titleView = findViewById(R.id.text_recipe_title);
        LinearLayout ingredientsContainer = findViewById(R.id.layout_ingredients_container);
        TextView instructionsView = findViewById(R.id.text_recipe_instructions);
        favoriteButton = findViewById(R.id.button_favorite);
        Button cookButton = findViewById(R.id.button_cook_recipe);

        titleView.setText(recipe.getName());
        instructionsView.setText(recipe.getInstructions());
        updateFavoriteIcon();

        List<RecipeIngredient> ingredients = dbHelper.getIngredientsForRecipe(recipeId);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (RecipeIngredient ingredient : ingredients) {
            View row = inflater.inflate(R.layout.item_recipe_ingredient_row, ingredientsContainer, false);
            TextView emojiView = row.findViewById(R.id.text_ingredient_row_emoji);
            TextView labelView = row.findViewById(R.id.text_ingredient_row_label);

            emojiView.setText(IngredientEmoji.forIngredient(ingredient.getIngredientName()));
            labelView.setText(String.format(Locale.getDefault(), "%s %s %s",
                    formatQuantity(ingredient.getRequiredQuantity()),
                    ingredient.getUnit(),
                    ingredient.getIngredientName()));

            ingredientsContainer.addView(row);
        }

        favoriteButton.setOnClickListener(v -> toggleFavorite());
        cookButton.setOnClickListener(v -> cookRecipe());
    }

    private void toggleFavorite() {
        boolean newState = !recipe.isFavorite();
        dbHelper.setRecipeFavorite(recipe.getId(), newState);
        recipe.setFavorite(newState);
        updateFavoriteIcon();
        Toast.makeText(this, newState ? "Added to favorites" : "Removed from favorites",
                Toast.LENGTH_SHORT).show();
    }

    private void updateFavoriteIcon() {
        favoriteButton.setImageResource(recipe.isFavorite()
                ? R.drawable.ic_star_filled
                : R.drawable.ic_star_outline);
    }

    private void cookRecipe() {
        boolean success = dbHelper.deductIngredientsForRecipe(recipe.getId());
        if (success) {
            Toast.makeText(this, "Enjoy! Used ingredients were deducted from your pantry.",
                    Toast.LENGTH_LONG).show();
            finish(); // back to Suggested Recipes, which will refresh via onResume
        } else {
            Toast.makeText(this,
                    "You no longer have every ingredient for this recipe — check your pantry.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String formatQuantity(double quantity) {
        if (quantity == Math.floor(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.format(Locale.getDefault(), "%.1f", quantity);
    }
}
