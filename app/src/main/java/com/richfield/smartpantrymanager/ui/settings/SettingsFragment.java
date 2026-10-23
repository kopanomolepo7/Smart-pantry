package com.richfield.smartpantrymanager.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.richfield.smartpantrymanager.R;

/**
 * Settings screen: expiring-soon alert toggle, a preferred default unit
 * (read by AddEditIngredientActivity when adding a new item), and an
 * appearance preference (read by SmartPantryApplication at startup, and
 * applied immediately here via AppCompatDelegate when changed).
 */
public class SettingsFragment extends Fragment {

    public static final String PREFS_NAME = "pantry_settings";
    public static final String KEY_EXPIRING_ALERTS = "expiring_alerts_enabled";
    public static final String KEY_DEFAULT_UNIT = "default_unit";
    public static final String KEY_DARK_MODE = "dark_mode_setting";
    public static final String[] AVAILABLE_UNITS = {"pcs", "g", "kg", "ml", "l"};

    private static final String[] APPEARANCE_LABELS = {"Follow system", "Light", "Dark"};
    private static final int[] APPEARANCE_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setUpExpiringAlertsSwitch(view);
        setUpDefaultUnitSpinner(view);
        setUpAppearanceSpinner(view);
    }

    private void setUpExpiringAlertsSwitch(View view) {
        SwitchCompat alertsSwitch = view.findViewById(R.id.switch_expiring_alerts);
        alertsSwitch.setChecked(prefs.getBoolean(KEY_EXPIRING_ALERTS, true));
        alertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_EXPIRING_ALERTS, isChecked).apply());
    }

    private void setUpDefaultUnitSpinner(View view) {
        Spinner unitSpinner = view.findViewById(R.id.spinner_default_unit);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, AVAILABLE_UNITS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        String savedUnit = prefs.getString(KEY_DEFAULT_UNIT, "pcs");
        for (int i = 0; i < AVAILABLE_UNITS.length; i++) {
            if (AVAILABLE_UNITS[i].equals(savedUnit)) {
                unitSpinner.setSelection(i);
                break;
            }
        }

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(KEY_DEFAULT_UNIT, AVAILABLE_UNITS[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setUpAppearanceSpinner(View view) {
        Spinner appearanceSpinner = view.findViewById(R.id.spinner_appearance);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, APPEARANCE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appearanceSpinner.setAdapter(adapter);

        int savedMode = prefs.getInt(KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        for (int i = 0; i < APPEARANCE_MODES.length; i++) {
            if (APPEARANCE_MODES[i] == savedMode) {
                appearanceSpinner.setSelection(i);
                break;
            }
        }

        appearanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int mode = APPEARANCE_MODES[position];
                prefs.edit().putInt(KEY_DARK_MODE, mode).apply();
                if (AppCompatDelegate.getDefaultNightMode() != mode) {
                    AppCompatDelegate.setDefaultNightMode(mode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
}
