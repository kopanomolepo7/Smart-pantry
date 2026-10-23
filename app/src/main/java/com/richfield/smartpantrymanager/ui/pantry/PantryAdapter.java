package com.richfield.smartpantrymanager.ui.pantry;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import com.richfield.smartpantrymanager.R;
import com.richfield.smartpantrymanager.model.PantryItem;
import com.richfield.smartpantrymanager.ui.settings.SettingsFragment;
import com.richfield.smartpantrymanager.util.ExpiryUtils;
import com.richfield.smartpantrymanager.util.IngredientEmoji;

/**
 * Binds the user's pantry_items data to the RecyclerView on the Pantry
 * screen. Tapping a row opens it for editing; tapping the delete button
 * asks the fragment to remove it. If the user has expiring-soon alerts
 * enabled (Settings), items within 3 days of their expiry date (or
 * already past it) get a small badge. Each ingredient also gets a
 * representative emoji (purely cosmetic, via IngredientEmoji).
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    public interface OnPantryItemListener {
        void onItemClicked(PantryItem item);
        void onDeleteClicked(PantryItem item);
    }

    private static final long EXPIRING_SOON_WINDOW_DAYS = 3;

    private List<PantryItem> items = new ArrayList<>();
    private final OnPantryItemListener listener;

    public PantryAdapter(OnPantryItemListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PantryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        PantryItem item = items.get(position);
        Context context = holder.itemView.getContext();

        String emoji = IngredientEmoji.forIngredient(item.getName());
        holder.emoji.setText(emoji);
        holder.name.setText(item.getName());

        String unit = item.getUnit() == null ? "" : item.getUnit();
        StringBuilder details = new StringBuilder();
        details.append(formatQuantity(item.getQuantity())).append(" ").append(unit);
        if (item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()) {
            details.append(" · expires ").append(item.getExpiryDate());
        }
        holder.details.setText(details.toString());

        Long daysUntilExpiry = ExpiryUtils.daysUntilExpiry(item.getExpiryDate());
        boolean alertsEnabled = isExpiringSoonAlertsEnabled(context);
        if (alertsEnabled && daysUntilExpiry != null && daysUntilExpiry <= EXPIRING_SOON_WINDOW_DAYS) {
            holder.expiryBadge.setVisibility(View.VISIBLE);
            holder.expiryBadge.setText(daysUntilExpiry < 0 ? "Expired" : "Expiring soon");
        } else {
            holder.expiryBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClicked(item));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatQuantity(double quantity) {
        if (quantity == Math.floor(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }

    private boolean isExpiringSoonAlertsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(SettingsFragment.KEY_EXPIRING_ALERTS, true);
    }

    static class PantryViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView emoji;
        final TextView name;
        final TextView details;
        final TextView expiryBadge;
        final ImageButton deleteButton;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            emoji = itemView.findViewById(R.id.text_ingredient_emoji);
            name = itemView.findViewById(R.id.text_ingredient_name);
            details = itemView.findViewById(R.id.text_ingredient_details);
            expiryBadge = itemView.findViewById(R.id.text_expiry_badge);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }
    }
}
