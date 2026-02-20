package it.unisa.skiscore;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.unisa.skiscore.api.ApiClient;
import it.unisa.skiscore.api.OwmForecastResponse;
import it.unisa.skiscore.ui.HourlySlotAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shows a vertical list of 3-hourly weather slots for a specific forecast day.
 *
 * Received via Intent:
 *   EXTRA_DATE        → String "YYYY-MM-DD"
 *   EXTRA_RESORT_NAME → String resort name
 *   EXTRA_LAT         → double latitude
 *   EXTRA_LON         → double longitude
 */
public class HourlyForecastActivity extends AppCompatActivity {

    public static final String EXTRA_DATE        = "hourly_date";
    public static final String EXTRA_RESORT_NAME = "hourly_resort_name";
    public static final String EXTRA_LAT         = "hourly_lat";
    public static final String EXTRA_LON         = "hourly_lon";

    private RecyclerView rvHourly;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private HourlySlotAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hourly_forecast);

        String date       = getIntent().getStringExtra(EXTRA_DATE);        // "YYYY-MM-DD"
        String resortName = getIntent().getStringExtra(EXTRA_RESORT_NAME);
        double lat        = getIntent().getDoubleExtra(EXTRA_LAT, 0);
        double lon        = getIntent().getDoubleExtra(EXTRA_LON, 0);

        ImageView ivBack  = findViewById(R.id.iv_hourly_back);
        TextView tvTitle  = findViewById(R.id.tv_hourly_title);
        rvHourly          = findViewById(R.id.rv_hourly);
        progressBar       = findViewById(R.id.hourly_progress);
        tvEmpty           = findViewById(R.id.tv_hourly_empty);

        // Title = "Resort – DD/MM"
        String displayDate = formatDateForTitle(date);
        tvTitle.setText((resortName != null ? resortName : "") + " – " + displayDate);
        ivBack.setOnClickListener(v -> finish());

        adapter = new HourlySlotAdapter();
        rvHourly.setLayoutManager(new LinearLayoutManager(this));
        rvHourly.setAdapter(adapter);

        fetchHourlyForecast(lat, lon, date);
    }

    /**
     * Calls OWM /forecast, filters items whose dtTxt starts with the requested date,
     * and passes them to the adapter.
     */
    private void fetchHourlyForecast(double lat, double lon, String targetDate) {
        showLoading(true);

        ApiClient.getForecastService().getForecast(
                lat, lon,
                ApiClient.OWM_API_KEY,
                "metric",
                "it"
        ).enqueue(new Callback<OwmForecastResponse>() {
            @Override
            public void onResponse(Call<OwmForecastResponse> call,
                                   Response<OwmForecastResponse> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showEmpty(true);
                    return;
                }

                List<OwmForecastResponse.ForecastItem> allItems = response.body().list;
                List<OwmForecastResponse.ForecastItem> daySlots = new ArrayList<>();

                if (allItems != null && targetDate != null) {
                    for (OwmForecastResponse.ForecastItem item : allItems) {
                        // dtTxt = "2025-02-20 12:00:00" → first 10 chars = "2025-02-20"
                        if (item.dtTxt != null && item.dtTxt.startsWith(targetDate)) {
                            daySlots.add(item);
                        }
                    }
                }

                if (daySlots.isEmpty()) {
                    showEmpty(true);
                } else {
                    adapter.setSlots(daySlots);
                }
            }

            @Override
            public void onFailure(Call<OwmForecastResponse> call, Throwable t) {
                showLoading(false);
                showEmpty(true);
                Toast.makeText(HourlyForecastActivity.this,
                        "Errore di rete: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvHourly.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvHourly.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /** "2025-02-20" → "20 Feb" */
    private String formatDateForTitle(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return dateStr != null ? dateStr : "";
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outFormat = new SimpleDateFormat("d MMM", Locale.ITALY);
            Date date = inFormat.parse(dateStr);
            if (date != null) {
                return outFormat.format(date);
            }
        } catch (Exception e) {
            // fallback
        }
        return dateStr;
    }
}
