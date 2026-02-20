package it.unisa.skiscore.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import it.unisa.skiscore.model.SkiResort;

/**
 * Manages favorite ski resorts using SharedPreferences with Gson serialization.
 */
public class FavoritesManager {

    private static final String PREFS_NAME = "skiscore_favorites";
    private static final String KEY_FAVORITES = "favorites_list";

    private final SharedPreferences prefs;
    private final Gson gson;

    public FavoritesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Get all favorite resorts.
     */
    public List<SkiResort> getFavorites() {
        String json = prefs.getString(KEY_FAVORITES, null);
        if (json == null) return new ArrayList<>();

        Type listType = new TypeToken<List<SkiResort>>() {}.getType();
        List<SkiResort> list = gson.fromJson(json, listType);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Save the favorites list.
     */
    private void saveFavorites(List<SkiResort> favorites) {
        String json = gson.toJson(favorites);
        prefs.edit().putString(KEY_FAVORITES, json).apply();
    }

    /**
     * Add a resort to favorites.
     */
    public void addFavorite(SkiResort resort) {
        List<SkiResort> favorites = getFavorites();
        resort.setFavorite(true);
        // Avoid duplicates
        for (SkiResort fav : favorites) {
            if (fav.getKey().equals(resort.getKey())) return;
        }
        favorites.add(resort);
        saveFavorites(favorites);
    }

    /**
     * Remove a resort from favorites.
     */
    public void removeFavorite(SkiResort resort) {
        List<SkiResort> favorites = getFavorites();
        favorites.removeIf(fav -> fav.getKey().equals(resort.getKey()));
        saveFavorites(favorites);
    }

    /**
     * Check if a resort is in favorites.
     */
    public boolean isFavorite(SkiResort resort) {
        List<SkiResort> favorites = getFavorites();
        for (SkiResort fav : favorites) {
            if (fav.getKey().equals(resort.getKey())) return true;
        }
        return false;
    }

    /**
     * Toggle favorite status.
     * @return true if the resort is now a favorite, false if removed
     */
    public boolean toggleFavorite(SkiResort resort) {
        if (isFavorite(resort)) {
            removeFavorite(resort);
            resort.setFavorite(false);
            return false;
        } else {
            addFavorite(resort);
            resort.setFavorite(true);
            return true;
        }
    }
}
