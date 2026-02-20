package it.unisa.skiscore.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.unisa.skiscore.HourlyForecastActivity;
import it.unisa.skiscore.R;
import it.unisa.skiscore.model.DailyForecast;
import it.unisa.skiscore.util.WeatherCodeMapper;

/**
 * Adapter for horizontal forecast days in detail view.
 * Each day card is clickable and opens HourlyForecastActivity.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private List<DailyForecast> forecasts = new ArrayList<>();

    // Resort name/lat/lon forwarded to HourlyForecastActivity
    private String resortName;
    private double lat, lon;

    public void setForecasts(List<DailyForecast> forecasts) {
        this.forecasts = forecasts != null ? forecasts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setResortInfo(String resortName, double lat, double lon) {
        this.resortName = resortName;
        this.lat = lat;
        this.lon = lon;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyForecast forecast = forecasts.get(position);
        holder.bind(forecast, resortName, lat, lon);
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvEmoji, tvTemp, tvSnow, tvWind;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay   = itemView.findViewById(R.id.tv_forecast_day);
            tvEmoji = itemView.findViewById(R.id.tv_forecast_emoji);
            tvTemp  = itemView.findViewById(R.id.tv_forecast_temp);
            tvSnow  = itemView.findViewById(R.id.tv_forecast_snow);
            tvWind  = itemView.findViewById(R.id.tv_forecast_wind);
        }

        void bind(DailyForecast f, String resortName, double lat, double lon) {
            String displayDate = f.getDate();
            if (displayDate != null && displayDate.length() >= 10) {
                try {
                    SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    SimpleDateFormat outFormat = new SimpleDateFormat("d MMM", Locale.ITALY);
                    Date date = inFormat.parse(f.getDate());
                    if (date != null) {
                        displayDate = outFormat.format(date);
                    }
                } catch (Exception e) {
                    // Fallback to original date string if parsing fails
                }
            }
            tvDay.setText(displayDate);
            tvEmoji.setText(WeatherCodeMapper.getWeatherEmoji(f.getWeatherMain()));
            tvTemp.setText(String.format("%.0f° / %.0f°", f.getTempMin(), f.getTempMax()));

            if (f.getSnowfall() > 0) {
                tvSnow.setText(String.format("🌨 %.0f mm", f.getSnowfall()));
                tvSnow.setVisibility(View.VISIBLE);
            } else {
                tvSnow.setVisibility(View.GONE);
            }

            tvWind.setText(String.format("💨 %.0f km/h", f.getWindSpeedMax() * 3.6));

            // Click → open HourlyForecastActivity for this day
            itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, HourlyForecastActivity.class);
                intent.putExtra(HourlyForecastActivity.EXTRA_DATE, f.getDate());
                intent.putExtra(HourlyForecastActivity.EXTRA_RESORT_NAME, resortName);
                intent.putExtra(HourlyForecastActivity.EXTRA_LAT, lat);
                intent.putExtra(HourlyForecastActivity.EXTRA_LON, lon);
                ctx.startActivity(intent);
            });
        }
    }
}
