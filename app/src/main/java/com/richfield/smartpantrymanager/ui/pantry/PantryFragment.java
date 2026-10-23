package com.richfield.smartpantrymanager.ui.pantry;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.richfield.smartpantrymanager.R;
import com.richfield.smartpantrymanager.database.DatabaseHelper;
import com.richfield.smartpantrymanager.model.PantryItem;
import com.richfield.smartpantrymanager.model.Recipe;
import com.richfield.smartpantrymanager.model.RecipeIngredient;
import com.richfield.smartpantrymanager.util.ExpiryUtils;
import com.richfield.smartpantrymanager.util.IngredientMatcher;

/**
 * Pantry List screen: shows all current pantry ingredients from the
 * database via a RecyclerView + custom Adapter, lets the user add, edit
 * and delete them, and shows a small stats banner ("N ingredients · N
 * ready to cook · N expiring soon") that updates alongside the list.
 */
public class PantryFragment extends Fragment implements PantryAdapter.OnPantryItemListener {

    private static final long EXPIRING_SOON_WINDOW_DAYS = 3;

    private DatabaseHelper dbHelper;
    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyStateText;
    private TextView statsText;
    private ActivityResultLauncher<Intent> addEditLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Registering here (rather than in onCreateView) is required by the
        // Activity Result API contract and ensures the launcher survives
        // fragment recreation.
        addEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> loadPantryData());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pantry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        recyclerView = view.findViewById(R.id.recycler_pantry);
        emptyStateText = view.findViewById(R.id.layout_empty_pantry);
        statsText = view.findViewById(R.id.text_pantry_stats);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_ingredient);

        adapter = new PantryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditIngredientActivity.class);
            addEditLauncher.launch(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload every time the screen becomes visible so edits/deletes
        // made elsewhere (or a fresh add) are always reflected.
        loadPantryData();
    }

    private void loadPantryData() {
        List<PantryItem> items = dbHelper.getAllPantryItems();
        adapter.setItems(items);

        boolean isEmpty = items.isEmpty();
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        updateStatsBanner(items);
    }

    private void updateStatsBanner(List<PantryItem> pantryItems) {
        int readyToCookCount = 0;
        for (Recipe recipe : dbHelper.getAllRecipes()) {
            List<RecipeIngredient> required = dbHelper.getIngredientsForRecipe(recipe.getId());
            if (IngredientMatcher.recipeMatchesPantry(required, pantryItems)) {
                readyToCookCount++;
            }
        }

        int expiringSoonCount = 0;
        for (PantryItem item : pantryItems) {
            Long days = ExpiryUtils.daysUntilExpiry(item.getExpiryDate());
            if (days != null && days <= EXPIRING_SOON_WINDOW_DAYS) {
                expiringSoonCount++;
            }
        }

        statsText.setText(String.format(Locale.getDefault(),
                "%d ingredient%s  ·  %d ready to cook  ·  %d expiring soon",
                pantryItems.size(), pantryItems.size() == 1 ? "" : "s",
                readyToCookCount, expiringSoonCount));
    }

    @Override
    public void onItemClicked(PantryItem item) {
        Intent intent = new Intent(requireContext(), AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_ITEM_ID, item.getId());
        addEditLauncher.launch(intent);
    }

    @Override
    public void onDeleteClicked(PantryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete ingredient")
                .setMessage("Remove " + item.getName() + " from your pantry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deletePantryItem(item.getId());
                    loadPantryData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
