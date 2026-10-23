package com.richfield.smartpantrymanager.ui.pantry;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.richfield.smartpantrymanager.R;
import com.richfield.smartpantrymanager.database.DatabaseHelper;
import com.richfield.smartpantrymanager.model.PantryItem;
import com.richfield.smartpantrymanager.ui.settings.SettingsFragment;

/**
 * Handles both adding a new pantry ingredient and editing an existing
 * one. Which mode we're in is decided purely by whether EXTRA_ITEM_ID
 * was passed in (an Intent extra, as required by the assignment).
 *
 * All input is validated before anything is written to the database:
 * name must not be empty, quantity must be a positive number, and an
 * optional expiry date must be a real yyyy-MM-dd date if provided.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private DatabaseHelper dbHelper;
    private long itemId = -1;

    private TextInputLayout nameLayout;
    private TextInputLayout quantityLayout;
    private TextInputLayout expiryLayout;
    private TextInputEditText nameInput;
    private TextInputEditText quantityInput;
    private TextInputEditText expiryInput;
    private Spinner unitSpinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        nameLayout = findViewById(R.id.layout_name);
        quantityLayout = findViewById(R.id.layout_quantity);
        expiryLayout = findViewById(R.id.layout_expiry);
        nameInput = findViewById(R.id.input_name);
        quantityInput = findViewById(R.id.input_quantity);
        expiryInput = findViewById(R.id.input_expiry);
        unitSpinner = findViewById(R.id.spinner_unit);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SettingsFragment.AVAILABLE_UNITS);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);

        if (itemId != -1) {
            setTitle("Edit Ingredient");
            loadExistingItem();
        } else {
            setTitle("Add Ingredient");
            preselectDefaultUnit();
        }

        findViewById(R.id.button_save).setOnClickListener(v -> validateAndSave());
    }

    private void preselectDefaultUnit() {
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        String defaultUnit = prefs.getString(SettingsFragment.KEY_DEFAULT_UNIT, "pcs");
        selectUnitInSpinner(defaultUnit);
    }

    private void loadExistingItem() {
        PantryItem item = dbHelper.getPantryItemById(itemId);
        if (item == null) {
            finish(); // shouldn't happen, but don't crash on a stale id
            return;
        }
        nameInput.setText(item.getName());
        quantityInput.setText(formatQuantity(item.getQuantity()));
        selectUnitInSpinner(item.getUnit());
        if (item.getExpiryDate() != null) {
            expiryInput.setText(item.getExpiryDate());
        }
    }

    private void selectUnitInSpinner(String unit) {
        for (int i = 0; i < SettingsFragment.AVAILABLE_UNITS.length; i++) {
            if (SettingsFragment.AVAILABLE_UNITS[i].equals(unit)) {
                unitSpinner.setSelection(i);
                return;
            }
        }
    }

    private String formatQuantity(double quantity) {
        if (quantity == Math.floor(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }

    private void validateAndSave() {
        nameLayout.setError(null);
        quantityLayout.setError(null);
        expiryLayout.setError(null);
        boolean valid = true;

        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameLayout.setError("Ingredient name is required");
            valid = false;
        }

        String quantityText = quantityInput.getText() == null ? "" : quantityInput.getText().toString().trim();
        double quantity = 0;
        if (quantityText.isEmpty()) {
            quantityLayout.setError("Quantity is required");
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(quantityText);
                if (quantity <= 0) {
                    quantityLayout.setError("Quantity must be greater than zero");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                quantityLayout.setError("Enter a valid number");
                valid = false;
            }
        }

        String expiry = expiryInput.getText() == null ? "" : expiryInput.getText().toString().trim();
        if (!expiry.isEmpty() && !isValidDate(expiry)) {
            expiryLayout.setError("Use format YYYY-MM-DD");
            valid = false;
        }

        if (!valid) {
            return;
        }

        String unit = (String) unitSpinner.getSelectedItem();
        String expiryOrNull = expiry.isEmpty() ? null : expiry;
        PantryItem item = new PantryItem(itemId == -1 ? 0 : itemId, name, quantity, unit, expiryOrNull);

        if (itemId == -1) {
            dbHelper.addPantryItem(item);
        } else {
            dbHelper.updatePantryItem(item);
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    private boolean isValidDate(String date) {
        try {
            DATE_FORMAT.setLenient(false);
            DATE_FORMAT.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
