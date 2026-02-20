package it.unisa.skiscore;

import it.unisa.skiscore.tracker.TrackerActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unisa.skiscore.api.ApiClient;
import it.unisa.skiscore.api.GeocodingResponse;
import it.unisa.skiscore.api.OwmCurrentResponse;
import it.unisa.skiscore.api.OwmForecastResponse;
import it.unisa.skiscore.databinding.ActivityMainBinding;
import it.unisa.skiscore.model.DailyForecast;
import it.unisa.skiscore.model.SkiResort;
import it.unisa.skiscore.model.WeatherData;
import it.unisa.skiscore.ui.ResortCardAdapter;
import it.unisa.skiscore.ui.SearchResultAdapter;
import it.unisa.skiscore.util.FavoritesManager;
import it.unisa.skiscore.util.SkiScoreCalculator;
import it.unisa.skiscore.util.WeatherCodeMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FavoritesManager favoritesManager;
    private ResortCardAdapter resortAdapter;
    private SearchResultAdapter searchAdapter;
    private List<SkiResort> displayedResorts = new ArrayList<>();
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        favoritesManager = new FavoritesManager(this);
        setupRecyclerViews();
        setupSearch();
        loadFavorites();

        // Tracker FAB
        binding.fabTracker.setOnClickListener(v ->
            startActivity(new Intent(this, TrackerActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupRecyclerViews() {
        // Resort cards list
        resortAdapter = new ResortCardAdapter();
        binding.rvResorts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResorts.setAdapter(resortAdapter);

        resortAdapter.setOnResortClickListener((resort, position) -> {
            Intent intent = new Intent(this, ResortDetailActivity.class);
            intent.putExtra("resort_name", resort.getName());
            intent.putExtra("resort_country", resort.getCountry());
            intent.putExtra("resort_lat", resort.getLatitude());
            intent.putExtra("resort_lon", resort.getLongitude());
            startActivity(intent);
        });

        resortAdapter.setOnFavoriteClickListener((resort, position) -> {
            boolean isFav = favoritesManager.toggleFavorite(resort);
            resort.setFavorite(isFav);
            resortAdapter.updateResort(position, resort);

            Toast.makeText(this,
                    isFav ? "Aggiunto ai preferiti ⭐" : "Rimosso dai preferiti",
                    Toast.LENGTH_SHORT).show();

            if (!isFav) {
                loadFavorites();
            }
        });

        // Search results dropdown
        searchAdapter = new SearchResultAdapter();
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(searchAdapter);

        searchAdapter.setOnResultClickListener(result -> {
            SkiResort resort = new SkiResort(
                    result.name,
                    result.country,
                    result.latitude,
                    result.longitude
            );
            resort.setFavorite(favoritesManager.isFavorite(resort));

            binding.rvSearchResults.setVisibility(View.GONE);
            binding.etSearch.setText("");
            hideKeyboard();

            displayedResorts.add(0, resort);
            resortAdapter.setResorts(displayedResorts);
            updateEmptyState();
            fetchWeatherForResort(resort, 0);
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, 400);
                } else {
                    binding.rvSearchResults.setVisibility(View.GONE);
                    searchAdapter.clear();
                }
            }
        });
    }

    private void performSearch(String query) {
        ApiClient.getGeocodingService()
                .searchLocation(query, 8, "it", "json")
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call,
                                           Response<GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().results != null) {
                            searchAdapter.setResults(response.body().results);
                            binding.rvSearchResults.setVisibility(View.VISIBLE);
                        } else {
                            binding.rvSearchResults.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        binding.rvSearchResults.setVisibility(View.GONE);
                    }
                });
    }

    private void loadFavorites() {
        List<SkiResort> favorites = favoritesManager.getFavorites();
        displayedResorts = new ArrayList<>(favorites);
        resortAdapter.setResorts(displayedResorts);
        updateEmptyState();

        for (int i = 0; i < displayedResorts.size(); i++) {
            fetchWeatherForResort(displayedResorts.get(i), i);
        }
    }

    /**
     * Fetch current weather from OWM for a resort.
     */
    private void fetchWeatherForResort(SkiResort resort, int position) {
        ApiClient.getWeatherService()
                .getCurrentWeather(
                        resort.getLatitude(),
                        resort.getLongitude(),
                        ApiClient.OWM_API_KEY,
                        "metric",
                        "it"
                )
                .enqueue(new Callback<OwmCurrentResponse>() {
                    @Override
                    public void onResponse(Call<OwmCurrentResponse> call,
                                           Response<OwmCurrentResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherData weatherData = mapOwmCurrentToWeatherData(response.body());
                            resort.setWeatherData(weatherData);
                            resort.setSkiScore(SkiScoreCalculator.calculateSkiScore(weatherData));
                            resort.setFavorite(favoritesManager.isFavorite(resort));

                            runOnUiThread(() -> resortAdapter.updateResort(position, resort));

                            // Also fetch forecast for this resort (populates forecast list)
                            fetchForecastForResort(resort, position);
                        }
                    }

                    @Override
                    public void onFailure(Call<OwmCurrentResponse> call, Throwable t) {
                        runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Errore meteo per " + resort.getName(),
                                    Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }

    /**
     * Fetch 5-day forecast from OWM for a resort.
     */
    private void fetchForecastForResort(SkiResort resort, int position) {
        ApiClient.getForecastService()
                .getForecast(
                        resort.getLatitude(),
                        resort.getLongitude(),
                        ApiClient.OWM_API_KEY,
                        "metric",
                        "it"
                )
                .enqueue(new Callback<OwmForecastResponse>() {
                    @Override
                    public void onResponse(Call<OwmForecastResponse> call,
                                           Response<OwmForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && resort.getWeatherData() != null) {
                            List<DailyForecast> forecasts = aggregateForecast(response.body());
                            resort.getWeatherData().setForecast(forecasts);
                            runOnUiThread(() -> resortAdapter.updateResort(position, resort));
                        }
                    }

                    @Override
                    public void onFailure(Call<OwmForecastResponse> call, Throwable t) {
                        // Forecast is secondary, no toast
                    }
                });
    }

    /**
     * Maps OWM Current Weather response to our WeatherData model.
     */
    static WeatherData mapOwmCurrentToWeatherData(OwmCurrentResponse owm) {
        WeatherData data = new WeatherData();

        if (owm.main != null) {
            data.setTempCurrent(owm.main.temp);
            data.setFeelsLike(owm.main.feelsLike);
            data.setTempMin(owm.main.tempMin);
            data.setTempMax(owm.main.tempMax);
            data.setHumidity(owm.main.humidity);
        }

        if (owm.wind != null) {
            data.setWindSpeed(owm.wind.speed);
            data.setWindGust(owm.wind.gust);
        }

        data.setVisibility(owm.visibility);

        if (owm.weather != null && !owm.weather.isEmpty()) {
            OwmCurrentResponse.Weather w = owm.weather.get(0);
            data.setWeatherMain(w.main);
            data.setWeatherDescription(w.description);
            data.setWeatherIcon(w.icon);
            data.setWeatherId(w.id);
        }

        if (owm.snow != null) {
            data.setSnowfall1h(owm.snow.oneHour);
            data.setSnowfall3h(owm.snow.threeHour);
        }

        return data;
    }

    /**
     * Aggregates OWM 3-hour forecast slots into daily forecasts.
     * Groups by date string (first 10 chars of dt_txt: "2025-02-20").
     */
    static List<DailyForecast> aggregateForecast(OwmForecastResponse response) {
        List<DailyForecast> dailyList = new ArrayList<>();
        if (response.list == null || response.list.isEmpty()) return dailyList;

        Map<String, DailyForecast> dayMap = new HashMap<>();
        List<String> dayOrder = new ArrayList<>();
        String[] dayNames = {"Oggi", "Domani", "Tra 2gg", "Tra 3gg", "Tra 4gg"};

        for (OwmForecastResponse.ForecastItem item : response.list) {
            if (item.dtTxt == null || item.dtTxt.length() < 10) continue;
            String dateKey = item.dtTxt.substring(0, 10);

            DailyForecast day = dayMap.get(dateKey);
            if (day == null) {
                day = new DailyForecast();
                day.setDate(dateKey);
                day.setTempMin(Double.MAX_VALUE);
                day.setTempMax(-Double.MAX_VALUE);
                day.setSnowfall(0);
                day.setWindSpeedMax(0);
                dayMap.put(dateKey, day);
                dayOrder.add(dateKey);
            }

            // Update min/max temp
            if (item.main != null) {
                day.setTempMin(Math.min(day.getTempMin(), item.main.tempMin));
                day.setTempMax(Math.max(day.getTempMax(), item.main.tempMax));
            }

            // Accumulate snowfall
            if (item.snow != null) {
                day.setSnowfall(day.getSnowfall() + item.snow.threeHour);
            }

            // Track max wind
            if (item.wind != null) {
                day.setWindSpeedMax(Math.max(day.getWindSpeedMax(), item.wind.speed));
            }

            // Use midday (12:00) weather as representative, or first available
            if (item.dtTxt.contains("12:00") || day.getWeatherMain() == null) {
                if (item.weather != null && !item.weather.isEmpty()) {
                    day.setWeatherMain(item.weather.get(0).main);
                    day.setWeatherDescription(item.weather.get(0).description);
                    day.setWeatherIcon(item.weather.get(0).icon);
                }
            }
        }

        // Build ordered list
        for (int i = 0; i < dayOrder.size() && i < 5; i++) {
            DailyForecast day = dayMap.get(dayOrder.get(i));
            day.setDayName(i < dayNames.length ? dayNames[i] : day.getDate());

            // Fix sentinel values
            if (day.getTempMin() == Double.MAX_VALUE) day.setTempMin(0);
            if (day.getTempMax() == -Double.MAX_VALUE) day.setTempMax(0);

            dailyList.add(day);
        }

        return dailyList;
    }

    private void updateEmptyState() {
        if (displayedResorts.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvResorts.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvResorts.setVisibility(View.VISIBLE);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}