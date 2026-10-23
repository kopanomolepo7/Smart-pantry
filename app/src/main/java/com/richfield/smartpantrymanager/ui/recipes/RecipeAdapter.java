package com.richfield.smartpantrymanager.ui.recipes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.richfield.smartpantrymanager.R;
import com.richfield.smartpantrymanager.model.Recipe;

/**
 * Displays a list of recipes — used for both the "Ready to Cook"
 * section (recipes that pass strict matching) and the "Almost There"
 * section (recipes missing exactly one ingredient). Which section it's
 * showing doesn't change the adapter's code, only the subtitle text
 * each recipe is given by the fragment (e.g. "Just need: cheese" vs
 * "You have everything for this").
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClicked(Recipe recipe);
    }

    private static final String DEFAULT_SUBTITLE = "You have everything for this — tap for details";

    private List<Recipe> recipes = new ArrayList<>();
    private Map<Long, String> subtitlesByRecipeId = new HashMap<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    /**
     * @param newRecipes the recipes to display, in order
     * @param subtitles  optional per-recipe subtitle override (e.g. "Just
     *                   need: cheese"). Any recipe not present in the map
     *                   falls back to the default "ready to cook" subtitle.
     */
    public void setItems(List<Recipe> newRecipes, Map<Long, String> subtitles) {
        this.recipes = newRecipes;
        this.subtitlesByRecipeId = subtitles != null ? subtitles : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.name.setText(recipe.getName());

        String subtitle = subtitlesByRecipeId.get(recipe.getId());
        holder.subtitle.setText(subtitle != null ? subtitle : DEFAULT_SUBTITLE);

        holder.favoriteIndicator.setVisibility(recipe.isFavorite() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onRecipeClicked(recipe));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView subtitle;
        final ImageView favoriteIndicator;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_recipe_name);
            subtitle = itemView.findViewById(R.id.text_recipe_subtitle);
            favoriteIndicator = itemView.findViewById(R.id.image_favorite_indicator);
        }
    }
}
