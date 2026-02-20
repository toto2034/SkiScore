package it.unisa.skiscore;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import it.unisa.skiscore.ui.DiaryFragment;
import it.unisa.skiscore.ui.HomeFragment;
import it.unisa.skiscore.ui.TrackerFragment;

/**
 * Main Activity — hosts the BottomNavigationView and swaps Fragments.
 *
 * Tabs:
 *   nav_home    → HomeFragment    (Meteo & Score)
 *   nav_tracker → TrackerFragment (Tracker ⏱)
 *   nav_diary   → DiaryFragment   (Diario Sciate)
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load HomeFragment at startup
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (id == R.id.nav_tracker) {
                selected = new TrackerFragment();
            } else if (id == R.id.nav_diary) {
                selected = new DiaryFragment();
            } else {
                return false;
            }

            loadFragment(selected);
            return true;
        });
    }

    /**
     * Replaces the current Fragment in fragment_container with the given one.
     * Uses REPLACE + commit — no back stack entry (tabs don't need back nav).
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}