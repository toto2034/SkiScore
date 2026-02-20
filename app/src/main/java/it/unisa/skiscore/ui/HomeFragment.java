package it.unisa.skiscore.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.unisa.skiscore.R;
import it.unisa.skiscore.ResortDetailActivity;
import it.unisa.skiscore.api.ApiClient;
import it.unisa.skiscore.api.GeocodingResponse;
import it.unisa.skiscore.api.OwmCurrentResponse;
import it.unisa.skiscore.model.SkiResort;
import it.unisa.skiscore.model.WeatherData;
import it.unisa.skiscore.util.FavoritesManager;
import it.unisa.skiscore.util.SkiScoreCalculator;
import it.unisa.skiscore.util.WeatherMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment — ricerca resort, caricamento meteo OWM, lista preferiti con SkiScore.
 */
public class HomeFragment extends Fragment {

    // Views
    private EditText etSearch;
    private RecyclerView rvSearchResults, rvResorts;
    private ProgressBar progressBar;
    private TextView tvSectionLabel;
    private LinearLayout llEmptyState;

    // Adapters
    private SearchResultAdapter searchResultAdapter;
    private ResortCardAdapter resortCardAdapter;

    // Data
    private FavoritesManager favoritesManager;
    private final List<SkiResort> displayedResorts = new ArrayList<>();

    // Search debounce
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private static final long SEARCH_DEBOUNCE_MS = 500L;
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch        = view.findViewById(R.id.et_search);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        rvResorts       = view.findViewById(R.id.rv_resorts);
        progressBar     = view.findViewById(R.id.progress_bar);
        tvSectionLabel  = view.findViewById(R.id.tv_section_label);
        llEmptyState    = view.findViewById(R.id.ll_empty_state);

        favoritesManager = new FavoritesManager(requireContext());

        setupSearchRecycler();
        setupResortRecycler();
        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    // ---- Setup ----

    private void setupSearchRecycler() {
        searchResultAdapter = new SearchResultAdapter();
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(searchResultAdapter);

        searchResultAdapter.setOnResultClickListener(result -> {
            hideKeyboard();
            rvSearchResults.setVisibility(View.GONE);
            etSearch.setText("");

            SkiResort resort = new SkiResort(result.getDisplayName(),
                    result.country, result.latitude, result.longitude);
            resort.setFavorite(favoritesManager.isFavorite(resort));
            addOrUpdateResort(resort);
            fetchWeatherForResort(resort);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                String query = s.toString().trim();
                if (query.length() < 2) {
                    rvSearchResults.setVisibility(View.GONE);
                    return;
                }
                pendingSearch = () -> searchResorts(query);
                searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupResortRecycler() {
        resortCardAdapter = new ResortCardAdapter();
        rvResorts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResorts.setAdapter(resortCardAdapter);

        resortCardAdapter.setOnResortClickListener((resort, position) -> {
            Intent intent = new Intent(requireContext(), ResortDetailActivity.class);
            intent.putExtra("resort_name", resort.getName());
            intent.putExtra("resort_country", resort.getCountry());
            intent.putExtra("resort_lat", resort.getLatitude());
            intent.putExtra("resort_lon", resort.getLongitude());
            startActivity(intent);
        });

        resortCardAdapter.setOnFavoriteClickListener((resort, position) -> {
            boolean isFav = favoritesManager.toggleFavorite(resort);
            resort.setFavorite(isFav);
            resortCardAdapter.updateResort(position, resort);
            Toast.makeText(requireContext(),
                    isFav ? "Aggiunto ai preferiti ⭐" : "Rimosso dai preferiti",
                    Toast.LENGTH_SHORT).show();
            if (!isFav) {
                displayedResorts.removeIf(r -> r.getKey().equals(resort.getKey()));
                resortCardAdapter.setResorts(new ArrayList<>(displayedResorts));
                updateEmptyState();
            }
        });
    }

    // ---- Data loading ----

    private void loadFavorites() {
        List<SkiResort> favorites = favoritesManager.getFavorites();
        tvSectionLabel.setText("⭐ I tuoi preferiti");
        displayedResorts.clear();
        displayedResorts.addAll(favorites);
        resortCardAdapter.setResorts(new ArrayList<>(displayedResorts));
        updateEmptyState();
        for (SkiResort resort : favorites) {
            fetchWeatherForResort(resort);
        }
    }

    private void addOrUpdateResort(SkiResort resort) {
        for (int i = 0; i < displayedResorts.size(); i++) {
            if (displayedResorts.get(i).getKey().equals(resort.getKey())) {
                displayedResorts.set(i, resort);
                resortCardAdapter.setResorts(new ArrayList<>(displayedResorts));
                return;
            }
        }
        displayedResorts.add(0, resort);
        resortCardAdapter.setResorts(new ArrayList<>(displayedResorts));
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = displayedResorts.isEmpty();
        llEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvResorts.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ---- Search ----

    private void searchResorts(String query) {
        ApiClient.getGeocodingService()
                .searchLocation(query, 8, "it", "json")
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call,
                                           Response<GeocodingResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null
                                && response.body().results != null) {
                            searchResultAdapter.setResults(response.body().results);
                            rvSearchResults.setVisibility(
                                    response.body().results.isEmpty() ? View.GONE : View.VISIBLE);
                        } else {
                            rvSearchResults.setVisibility(View.GONE);
                        }
                    }
                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        rvSearchResults.setVisibility(View.GONE);
                    }
                });
    }

    // ---- Weather Fetch ----

    private void fetchWeatherForResort(SkiResort resort) {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getWeatherService()
                .getCurrentWeather(resort.getLatitude(), resort.getLongitude(),
                        ApiClient.OWM_API_KEY, "metric", "it")
                .enqueue(new Callback<OwmCurrentResponse>() {
                    @Override
                    public void onResponse(Call<OwmCurrentResponse> call,
                                           Response<OwmCurrentResponse> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherData data = WeatherMapper.mapOwmCurrentToWeatherData(response.body());
                            resort.setWeatherData(data);
                            resort.setSkiScore(SkiScoreCalculator.calculateSkiScore(data));
                            for (int i = 0; i < displayedResorts.size(); i++) {
                                if (displayedResorts.get(i).getKey().equals(resort.getKey())) {
                                    displayedResorts.set(i, resort);
                                    resortCardAdapter.updateResort(i, resort);
                                    break;
                                }
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<OwmCurrentResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    // ---- Utility ----

    private void hideKeyboard() {
        if (getActivity() == null || etSearch == null) return;
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }
}
