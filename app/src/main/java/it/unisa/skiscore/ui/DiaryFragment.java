package it.unisa.skiscore.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unisa.skiscore.R;
import it.unisa.skiscore.db.AppDatabase;
import it.unisa.skiscore.db.SkiSession;
import it.unisa.skiscore.db.SkiSessionDao;

/**
 * DiaryFragment — mostra lo storico delle sessioni sci dal database Room.
 * Statistiche stagionali: km totali, velocità massima, giorni sciati.
 */
public class DiaryFragment extends Fragment {

    private TextView tvTotalDistance, tvMaxSpeedSeason, tvDaysSkied, tvDiaryEmpty;
    private RecyclerView rvSessions;
    private DiarySessionAdapter sessionAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalDistance  = view.findViewById(R.id.tv_total_distance);
        tvMaxSpeedSeason = view.findViewById(R.id.tv_max_speed_season);
        tvDaysSkied      = view.findViewById(R.id.tv_days_skied);
        tvDiaryEmpty     = view.findViewById(R.id.tv_diary_empty);
        rvSessions       = view.findViewById(R.id.rv_sessions);

        sessionAdapter = new DiarySessionAdapter();
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(sessionAdapter);

        loadData();
    }

    private void loadData() {
        SkiSessionDao dao = AppDatabase.getInstance(requireContext()).skiSessionDao();

        // Osserva la lista sessioni con LiveData — si aggiorna automaticamente
        dao.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                rvSessions.setVisibility(View.GONE);
                tvDiaryEmpty.setVisibility(View.VISIBLE);
            } else {
                rvSessions.setVisibility(View.VISIBLE);
                tvDiaryEmpty.setVisibility(View.GONE);
                sessionAdapter.setSessions(sessions);
            }
        });

        // Statistiche aggregate — eseguite in background
        executor.execute(() -> {
            float totalKm  = dao.getTotalDistance();
            float maxSpeed = dao.getMaxSpeedSeason();
            int   days     = dao.getTotalDaysSkied();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f km", totalKm));
                    tvMaxSpeedSeason.setText(String.format(Locale.getDefault(), "%.0f km/h", maxSpeed));
                    tvDaysSkied.setText(String.valueOf(days));
                });
            }
        });
    }

    // ---- Inner adapter for sessions list ----

    private static class DiarySessionAdapter
            extends RecyclerView.Adapter<DiarySessionAdapter.SessionViewHolder> {

        private List<SkiSession> sessions;
        private final SimpleDateFormat sdf =
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        void setSessions(List<SkiSession> sessions) {
            this.sessions = sessions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Simple inline row layout built programmatically to avoid extra XML
            android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
            row.setOrientation(android.widget.LinearLayout.VERTICAL);
            row.setBackgroundColor(0xFF141E33);
            row.setPadding(32, 24, 32, 24);
            android.view.ViewGroup.MarginLayoutParams params =
                    new android.view.ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 8);
            row.setLayoutParams(params);
            return new SessionViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
            holder.bind(sessions.get(position));
        }

        @Override
        public int getItemCount() {
            return sessions == null ? 0 : sessions.size();
        }

        class SessionViewHolder extends RecyclerView.ViewHolder {
            android.widget.LinearLayout container;

            SessionViewHolder(View itemView) {
                super(itemView);
                container = (android.widget.LinearLayout) itemView;
            }

            void bind(SkiSession s) {
                container.removeAllViews();

                TextView tvDate = makeText(sdf.format(new Date(s.date)), 13, 0x80B0C4D8);
                TextView tvStats = makeText(
                        String.format(Locale.getDefault(),
                                "📏 %.2f km  ·  ⚡ %.0f km/h max  ·  ⌀ %.0f km/h",
                                s.totalDistance, s.maxSpeed, s.avgSpeed),
                        16, 0xFFE8EAF6);
                tvStats.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                TextView tvDuration = makeText(
                        "⏱ " + formatDuration(s.duration), 13, 0x80B0C4D8);

                container.addView(tvDate);
                container.addView(tvStats);
                container.addView(tvDuration);
            }

            private TextView makeText(String text, int spSize, int color) {
                TextView tv = new TextView(container.getContext());
                tv.setText(text);
                tv.setTextSize(spSize);
                tv.setTextColor(color);
                return tv;
            }

            private String formatDuration(long ms) {
                long s = ms / 1000, h = s / 3600, m = (s % 3600) / 60;
                return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s % 60);
            }
        }
    }
}
