package com.richfield.smartpantrymanager.ui.recipes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.richfield.smartpantrymanager.R;
import com.richfield.smartpantrymanager.database.DatabaseHelper;
import com.richfield.smartpantrymanager.model.PantryItem;
import com.richfield.smartpantrymanager.model.Recipe;
import com.richfield.smartpantrymanager.model.RecipeIngredient;
import com.richfield.smartpantrymanager.util.IngredientMatcher;

/**
 * Suggested Recipes screen. Runs the strict-matching algorithm
 * (IngredientMatcher) against the current pantry every time this
 * screen becomes visible, so it always reflects the latest pantry
 * contents — add or remove an ingredient on the Pantry tab and come
 * back here to see recipes appear or disappear accordingly.
 *
 * Shows two clearly separate sections:
 *  - "Ready to Cook": recipes passing the strict match (0 unmet ingredients)
 *  - "Almost There" (optional stretch feature): recipes missing exactly
 *    one ingredient, each showing which one
 *
 * A "Favorites only" checkbox filters both sections down to recipes the
 * user has starred on the Recipe Detail screen.
 */
public class SuggestedRecipesFragment extends Fragment
        implements RecipeAdapter.OnRecipeClickListener {

    private DatabaseHelper dbHelper;
    private RecipeAdapter readyAdapter;
    private RecipeAdapter almostThereAdapter;

    private RecyclerView readyRecycler;
    private View readyEmptyState;
    private RecyclerView almostThereRecycler;
    private TextView almostThereEmptyText;
    private CheckBox favoritesOnlyCheckbox;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        readyRecycler = view.findViewById(R.id.recycler_recipes);
        readyEmptyState = view.findViewById(R.id.layout_empty_recipes);
        almostThereRecycler = view.findViewById(R.id.recycler_almost_there);
        almostThereEmptyText = view.findViewById(R.id.text_empty_almost_there);
        favoritesOnlyCheckbox = view.findViewById(R.id.checkbox_favorites_only);

        readyAdapter = new RecipeAdapter(this);
        readyRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        readyRecycler.setAdapter(readyAdapter);

        almostThereAdapter = new RecipeAdapter(this);
        almostThereRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        almostThereRecycler.setAdapter(almostThereAdapter);

        favoritesOnlyCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> loadRecipeLists());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecipeLists();
    }

    private void loadRecipeLists() {
        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        List<Recipe> allRecipes = dbHelper.getAllRecipes();

        boolean favoritesOnly = favoritesOnlyCheckbox.isChecked();
        if (favoritesOnly) {
            List<Recipe> favoritesOnlyList = new ArrayList<>();
            for (Recipe recipe : allRecipes) {
                if (recipe.isFavorite()) {
                    favoritesOnlyList.add(recipe);
                }
            }
            allRecipes = favoritesOnlyList;
        }

        List<Recipe> readyToCook = new ArrayList<>();
        List<Recipe> almostThere = new ArrayList<>();
        Map<Long, String> almostThereSubtitles = new HashMap<>();

        for (Recipe recipe : allRecipes) {
            List<RecipeIngredient> required = dbHelper.getIngredientsForRecipe(recipe.getId());
            int unmetCount = IngredientMatcher.countUnmetRequirements(required, pantryItems);

            if (unmetCount == 0) {
                readyToCook.add(recipe);
            } else if (unmetCount == 1) {
                almostThere.add(recipe);
                List<String> missing = IngredientMatcher.unmetIngredientNames(required, pantryItems);
                String missingName = missing.isEmpty() ? "one ingredient" : missing.get(0);
                almostThereSubtitles.put(recipe.getId(),
                        String.format(Locale.getDefault(), "Just need: %s", missingName));
            }
        }

        readyAdapter.setItems(readyToCook, null); // null -> default "ready to cook" subtitle
        almostThereAdapter.setItems(almostThere, almostThereSubtitles);

        readyEmptyState.setVisibility(readyToCook.isEmpty() ? View.VISIBLE : View.GONE);
        readyRecycler.setVisibility(readyToCook.isEmpty() ? View.GONE : View.VISIBLE);

        almostThereEmptyText.setVisibility(almostThere.isEmpty() ? View.VISIBLE : View.GONE);
        almostThereRecycler.setVisibility(almostThere.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onRecipeClicked(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}
