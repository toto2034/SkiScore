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
import it.unisa.skiscore.api.GeocodingResponse;

/**
 * Adapter for the search results dropdown list.
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<GeocodingResponse.GeoResult> results = new ArrayList<>();
    private OnResultClickListener listener;

    public interface OnResultClickListener {
        void onResultClick(GeocodingResponse.GeoResult result);
    }

    public void setOnResultClickListener(OnResultClickListener listener) {
        this.listener = listener;
    }

    public void setResults(List<GeocodingResponse.GeoResult> results) {
        this.results = results != null ? results : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void clear() {
        this.results.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GeocodingResponse.GeoResult result = results.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvElevation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_search_name);
            tvElevation = itemView.findViewById(R.id.tv_search_elevation);
        }

        void bind(GeocodingResponse.GeoResult result) {
            tvName.setText(result.getDisplayName());
            if (result.elevation > 0) {
                tvElevation.setText(String.format("%.0f m", result.elevation));
            } else {
                tvElevation.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onResultClick(result);
            });
        }
    }
}
