package com.richfield.smartpantrymanager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.richfield.smartpantrymanager.ui.pantry.PantryFragment;
import com.richfield.smartpantrymanager.ui.recipes.SuggestedRecipesFragment;
import com.richfield.smartpantrymanager.ui.settings.SettingsFragment;

/**
 * Single host Activity for the app. Holds the toolbar and bottom
 * navigation, and swaps in the Pantry / Suggested Recipes / Settings
 * fragments as the user taps between tabs.
 *
 * We keep navigation logic here and screen-specific logic inside each
 * Fragment, so this class stays small even as more features are added.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // Default tab on first launch.
        if (savedInstanceState == null) {
            showFragment(new PantryFragment(), "Pantry");
        }
    }

    private boolean onNavItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_pantry) {
            showFragment(new PantryFragment(), "Pantry");
            return true;
        } else if (id == R.id.nav_recipes) {
            showFragment(new SuggestedRecipesFragment(), "Suggested Recipes");
            return true;
        } else if (id == R.id.nav_settings) {
            showFragment(new SettingsFragment(), "Settings");
            return true;
        }
        return false;
    }

    private void showFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }
}
