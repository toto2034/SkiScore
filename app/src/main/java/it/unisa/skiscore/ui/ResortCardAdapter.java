package it.unisa.skiscore.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.unisa.skiscore.R;
import it.unisa.skiscore.model.SkiResort;
import it.unisa.skiscore.model.WeatherData;
import it.unisa.skiscore.util.WeatherCodeMapper;

/**
 * RecyclerView adapter for ski resort cards in the main list.
 */
public class ResortCardAdapter extends RecyclerView.Adapter<ResortCardAdapter.ViewHolder> {

    private List<SkiResort> resorts = new ArrayList<>();
    private OnResortClickListener clickListener;
    private OnFavoriteClickListener favoriteListener;

    public interface OnResortClickListener {
        void onResortClick(SkiResort resort, int position);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(SkiResort resort, int position);
    }

    public void setOnResortClickListener(OnResortClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    public void setResorts(List<SkiResort> resorts) {
        this.resorts = resorts != null ? resorts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateResort(int position, SkiResort resort) {
        if (position >= 0 && position < resorts.size()) {
            resorts.set(position, resort);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resort_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SkiResort resort = resorts.get(position);
        holder.bind(resort, position);
    }

    @Override
    public int getItemCount() {
        return resorts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLocation, tvTemp, tvCondition;
        TextView tvSnowValue, tvWindValue, tvVisibilityEmoji, tvVisibilityValue, tvTempRange;
        SkiScoreView skiScoreView;
        ImageView ivFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_resort_name);
            tvLocation = itemView.findViewById(R.id.tv_resort_location);
            tvTemp = itemView.findViewById(R.id.tv_current_temp);
            tvCondition = itemView.findViewById(R.id.tv_condition);
            tvSnowValue = itemView.findViewById(R.id.tv_snow_value);
            tvWindValue = itemView.findViewById(R.id.tv_wind_value);
            tvVisibilityEmoji = itemView.findViewById(R.id.tv_visibility_emoji);
            tvVisibilityValue = itemView.findViewById(R.id.tv_visibility_value);
            tvTempRange = itemView.findViewById(R.id.tv_temp_range);
            skiScoreView = itemView.findViewById(R.id.ski_score_view);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
        }

        void bind(SkiResort resort, int position) {
            tvName.setText(resort.getName());
            tvLocation.setText(resort.getCountry() != null ? resort.getCountry() : "");

            // Favorite icon
            ivFavorite.setImageResource(resort.isFavorite()
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);

            WeatherData weather = resort.getWeatherData();
            if (weather != null) {
                tvTemp.setText(String.format("%.0f°C", weather.getTempCurrent()));
                tvCondition.setText(weather.getWeatherDescription() != null
                        ? weather.getWeatherDescription()
                        : WeatherCodeMapper.getConditionText(weather.getWeatherMain()));

                // Snow pill: show snow.1h or snow.3h
                if (weather.getSnowfall1h() > 0) {
                    tvSnowValue.setText(String.format("%.1f mm/h", weather.getSnowfall1h()));
                } else if (weather.getSnowfall3h() > 0) {
                    tvSnowValue.setText(String.format("%.1f mm", weather.getSnowfall3h()));
                } else {
                    tvSnowValue.setText("0 mm");
                }

                // Wind pill (m/s → km/h for display)
                tvWindValue.setText(String.format("%.0f km/h", weather.getWindSpeed() * 3.6));

                // Visibility pill
                tvVisibilityEmoji.setText(WeatherCodeMapper.getWeatherEmoji(weather.getWeatherMain()));
                tvVisibilityValue.setText(WeatherCodeMapper.getConditionText(weather.getWeatherMain()));

                // Temp range pill
                tvTempRange.setText(String.format("%.0f°/%.0f°", weather.getTempMin(), weather.getTempMax()));

                skiScoreView.setScoreImmediate(resort.getSkiScore());
            } else {
                tvTemp.setText("--°C");
                tvCondition.setText("Caricamento…");
                tvSnowValue.setText("--");
                tvWindValue.setText("--");
                tvVisibilityEmoji.setText("🌡️");
                tvVisibilityValue.setText("--");
                tvTempRange.setText("--°/--°");
                skiScoreView.setScoreImmediate(0);
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onResortClick(resort, position);
            });

            ivFavorite.setOnClickListener(v -> {
                if (favoriteListener != null) favoriteListener.onFavoriteClick(resort, position);
            });
        }
    }
}
