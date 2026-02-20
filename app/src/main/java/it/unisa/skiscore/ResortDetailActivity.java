package it.unisa.skiscore;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.unisa.skiscore.api.ApiClient;
import it.unisa.skiscore.api.OwmCurrentResponse;
import it.unisa.skiscore.api.OwmForecastResponse;
import it.unisa.skiscore.model.DailyForecast;
import it.unisa.skiscore.model.SkiResort;
import it.unisa.skiscore.model.WeatherData;
import it.unisa.skiscore.ui.ForecastAdapter;
import it.unisa.skiscore.ui.SkiScoreView;
import it.unisa.skiscore.util.FavoritesManager;
import it.unisa.skiscore.util.SkiScoreCalculator;
import it.unisa.skiscore.util.WeatherCodeMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Full weather dashboard for a single ski resort.
 * Fetches data from OpenWeatherMap Current + Forecast APIs.
 */
public class ResortDetailActivity extends AppCompatActivity {

    private SkiResort resort;
    private FavoritesManager favoritesManager;
    private ForecastAdapter forecastAdapter;

    // Views
    private SkiScoreView scoreView;
    private TextView tvName, tvLocation;
    private TextView tvTemp, tvFeelsLike, tvTempRange;
    private TextView tvWind, tvWindStatus;
    private TextView tvSnowFresh, tvSnowDepth;
    private TextView tvVisibility, tvVisibilityEmoji;
    private ImageView ivBack, ivFav;
    private ProgressBar progressBar;
    private RecyclerView rvForecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resort_detail);

        View root = findViewById(R.id.ll_detail_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft() + systemBars.left,
                    systemBars.top + 20,
                    v.getPaddingRight() + systemBars.right,
                    v.getPaddingBottom() + systemBars.bottom
            );
            return insets;
        });

        favoritesManager = new FavoritesManager(this);
        initViews();
        loadResortFromIntent();
        setupClickListeners();
        fetchWeather();
    }

    private void initViews() {
        scoreView = findViewById(R.id.detail_score_view);
        tvName = findViewById(R.id.tv_detail_name);
        tvLocation = findViewById(R.id.tv_detail_location);
        tvTemp = findViewById(R.id.tv_detail_temp);
        tvFeelsLike = findViewById(R.id.tv_detail_feels_like);
        tvTempRange = findViewById(R.id.tv_detail_temp_range);
        tvWind = findViewById(R.id.tv_detail_wind);
        tvWindStatus = findViewById(R.id.tv_detail_wind_status);
        tvSnowFresh = findViewById(R.id.tv_detail_snow_fresh);
        tvSnowDepth = findViewById(R.id.tv_detail_snow_depth);
        tvVisibility = findViewById(R.id.tv_detail_visibility);
        tvVisibilityEmoji = findViewById(R.id.tv_detail_visibility_emoji);
        ivBack = findViewById(R.id.iv_back);
        ivFav = findViewById(R.id.iv_detail_fav);
        progressBar = findViewById(R.id.detail_progress);
        rvForecast = findViewById(R.id.rv_forecast);

        forecastAdapter = new ForecastAdapter();
        rvForecast.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvForecast.setAdapter(forecastAdapter);
    }

    private void loadResortFromIntent() {
        String name = getIntent().getStringExtra("resort_name");
        String country = getIntent().getStringExtra("resort_country");
        double lat = getIntent().getDoubleExtra("resort_lat", 0);
        double lon = getIntent().getDoubleExtra("resort_lon", 0);

        resort = new SkiResort(name, country, lat, lon);
        resort.setFavorite(favoritesManager.isFavorite(resort));

        tvName.setText(name);
        tvLocation.setText(country != null ? country : "");
        updateFavoriteIcon();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivFav.setOnClickListener(v -> {
            boolean isFav = favoritesManager.toggleFavorite(resort);
            resort.setFavorite(isFav);
            updateFavoriteIcon();
            Toast.makeText(this,
                    isFav ? "Aggiunto ai preferiti ⭐" : "Rimosso dai preferiti",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFavoriteIcon() {
        ivFav.setImageResource(resort.isFavorite()
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
    }

    /**
     * Fetch current weather from OWM.
     */
    private void fetchWeather() {
        progressBar.setVisibility(View.VISIBLE);

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
                            WeatherData data = MainActivity.mapOwmCurrentToWeatherData(response.body());
                            resort.setWeatherData(data);
                            resort.setSkiScore(SkiScoreCalculator.calculateSkiScore(data));

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                displayWeatherData(data);
                            });

                            // Also fetch forecast
                            fetchForecast();
                        } else {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(ResortDetailActivity.this,
                                        getString(R.string.error_generic),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<OwmCurrentResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ResortDetailActivity.this,
                                    getString(R.string.error_network),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Fetch 5-day forecast from OWM.
     */
    private void fetchForecast() {
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
                        if (response.isSuccessful() && response.body() != null) {
                            List<DailyForecast> forecasts =
                                    MainActivity.aggregateForecast(response.body());
                            if (resort.getWeatherData() != null) {
                                resort.getWeatherData().setForecast(forecasts);
                            }
                            runOnUiThread(() -> forecastAdapter.setForecasts(forecasts));
                        }
                    }

                    @Override
                    public void onFailure(Call<OwmForecastResponse> call, Throwable t) {
                        // Forecast is secondary
                    }
                });
    }

    private void displayWeatherData(WeatherData data) {
        // Score ring (animated)
        scoreView.setScore(resort.getSkiScore());

        // Temperature
        tvTemp.setText(String.format("%.1f°C", data.getTempCurrent()));
        tvFeelsLike.setText(String.format("Percepita: %.1f°C", data.getFeelsLike()));
        tvTempRange.setText(String.format("Min: %.0f° / Max: %.0f°", data.getTempMin(), data.getTempMax()));

        // Wind (m/s → also show km/h for readability)
        double windKmh = data.getWindSpeed() * 3.6;
        tvWind.setText(String.format("%.1f m/s (%.0f km/h)", data.getWindSpeed(), windKmh));
        if (data.getWindSpeed() < 3) {
            tvWindStatus.setText("Calmo ✅");
        } else if (data.getWindSpeed() < 7) {
            tvWindStatus.setText("Moderato ⚠️");
        } else if (data.getWindSpeed() < 12) {
            tvWindStatus.setText("Forte ⚠️");
        } else {
            tvWindStatus.setText("Pericoloso ❌");
        }

        // Snow (show snow.1h or snow.3h)
        if (data.getSnowfall1h() > 0) {
            tvSnowFresh.setText(String.format("%.1f mm/h", data.getSnowfall1h()));
        } else if (data.getSnowfall3h() > 0) {
            tvSnowFresh.setText(String.format("%.1f mm/3h", data.getSnowfall3h()));
        } else {
            tvSnowFresh.setText("0 mm");
        }

        // Visibility
        if (data.getVisibility() > 0) {
            if (data.getVisibility() >= 1000) {
                tvSnowDepth.setText(String.format("Visibilità: %.1f km", data.getVisibility() / 1000.0));
            } else {
                tvSnowDepth.setText(String.format("Visibilità: %d m", data.getVisibility()));
            }
        } else {
            tvSnowDepth.setText("Visibilità: N/D");
        }

        // Weather condition
        tvVisibility.setText(data.getWeatherDescription() != null
                ? capitalizeFirst(data.getWeatherDescription())
                : WeatherCodeMapper.getConditionText(data.getWeatherMain()));
        tvVisibilityEmoji.setText(WeatherCodeMapper.getWeatherEmoji(data.getWeatherMain()));

        // Forecast
        if (data.getForecast() != null) {
            forecastAdapter.setForecasts(data.getForecast());
        }
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
