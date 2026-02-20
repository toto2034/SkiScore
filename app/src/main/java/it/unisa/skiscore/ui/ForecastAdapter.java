package it.unisa.skiscore.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.unisa.skiscore.R;
import it.unisa.skiscore.model.DailyForecast;
import it.unisa.skiscore.util.WeatherCodeMapper;

/**
 * Adapter for horizontal forecast days in detail view.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private List<DailyForecast> forecasts = new ArrayList<>();

    public void setForecasts(List<DailyForecast> forecasts) {
        this.forecasts = forecasts != null ? forecasts : new ArrayList<>();
        notifyDataSetChanged();
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
        holder.bind(forecast);
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvEmoji, tvTemp, tvSnow, tvWind;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_forecast_day);
            tvEmoji = itemView.findViewById(R.id.tv_forecast_emoji);
            tvTemp = itemView.findViewById(R.id.tv_forecast_temp);
            tvSnow = itemView.findViewById(R.id.tv_forecast_snow);
            tvWind = itemView.findViewById(R.id.tv_forecast_wind);
        }

        void bind(DailyForecast f) {
            tvDay.setText(f.getDayName() != null ? f.getDayName() : f.getDate());
            tvEmoji.setText(WeatherCodeMapper.getWeatherEmoji(f.getWeatherMain()));
            tvTemp.setText(String.format("%.0f° / %.0f°", f.getTempMin(), f.getTempMax()));

            if (f.getSnowfall() > 0) {
                tvSnow.setText(String.format("🌨 %.0f mm", f.getSnowfall()));
                tvSnow.setVisibility(View.VISIBLE);
            } else {
                tvSnow.setVisibility(View.GONE);
            }

            // Wind: m/s → km/h
            tvWind.setText(String.format("💨 %.0f km/h", f.getWindSpeedMax() * 3.6));
        }
    }
}
