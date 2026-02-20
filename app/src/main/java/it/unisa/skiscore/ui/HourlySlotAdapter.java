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
import it.unisa.skiscore.api.OwmForecastResponse;
import it.unisa.skiscore.util.WeatherCodeMapper;

/**
 * Adapter for the vertical list of 3-hour forecast slots in HourlyForecastActivity.
 */
public class HourlySlotAdapter extends RecyclerView.Adapter<HourlySlotAdapter.ViewHolder> {

    private List<OwmForecastResponse.ForecastItem> slots = new ArrayList<>();

    public void setSlots(List<OwmForecastResponse.ForecastItem> slots) {
        this.slots = slots != null ? slots : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_slot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(slots.get(position));
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvEmoji, tvDesc, tvTemp, tvSnow, tvWind;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime  = itemView.findViewById(R.id.tv_hourly_time);
            tvEmoji = itemView.findViewById(R.id.tv_hourly_emoji);
            tvDesc  = itemView.findViewById(R.id.tv_hourly_desc);
            tvTemp  = itemView.findViewById(R.id.tv_hourly_temp);
            tvSnow  = itemView.findViewById(R.id.tv_hourly_snow);
            tvWind  = itemView.findViewById(R.id.tv_hourly_wind);
        }

        void bind(OwmForecastResponse.ForecastItem item) {
            // dtTxt format: "2025-02-20 12:00:00"  →  extract "12:00"
            String time = "??:??";
            if (item.dtTxt != null && item.dtTxt.length() >= 16) {
                time = item.dtTxt.substring(11, 16);
            }
            tvTime.setText(time);

            // Weather emoji + Italian description (OWM already returns lang=it)
            String weatherMain = "";
            String desc = "";
            if (item.weather != null && !item.weather.isEmpty()) {
                weatherMain = item.weather.get(0).main;
                desc = item.weather.get(0).description;
                if (desc != null && !desc.isEmpty()) {
                    desc = Character.toUpperCase(desc.charAt(0)) + desc.substring(1);
                }
            }
            tvEmoji.setText(WeatherCodeMapper.getWeatherEmoji(weatherMain));
            tvDesc.setText(desc);

            // Temperature
            if (item.main != null) {
                tvTemp.setText(String.format("%.0f°", item.main.temp));
            }

            // Snow (3h accumulation) - show only if > 0
            double snowMm = 0;
            if (item.snow != null) snowMm = item.snow.threeHour;
            if (snowMm > 0) {
                tvSnow.setText(String.format("🌨 %.0fmm", snowMm));
                tvSnow.setVisibility(View.VISIBLE);
            } else {
                tvSnow.setVisibility(View.GONE);
            }

            // Wind (m/s → km/h)
            if (item.wind != null) {
                tvWind.setText(String.format("💨%.0f", item.wind.speed * 3.6));
            }
        }
    }
}
