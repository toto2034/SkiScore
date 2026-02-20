package it.unisa.skiscore.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;

import it.unisa.skiscore.R;
import it.unisa.skiscore.db.AppDatabase;
import it.unisa.skiscore.db.SkiSession;
import it.unisa.skiscore.db.SkiSessionDao;
import it.unisa.skiscore.tracker.SkiLocationService;

/**
 * TrackerFragment — tutta la logica GPS tracker nella tab "Tracker".
 * Contiene: permessi, avvio/arresto Service, BroadcastReceiver, cronometro, SOS, salvataggio Room.
 */
public class TrackerFragment extends Fragment {

    // Views
    private TextView tvSpeed, tvDistance, tvTime, tvMaxSpeed, tvAvgSpeed, tvChairlift;
    private MaterialButton btnStartStop, btnSos;

    private boolean isTracking = false;

    // Executor per Room (mai sul Main Thread)
    private final java.util.concurrent.ExecutorService dbExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    // Cache ultime stats per salvataggio DB
    private float lastSpeed = 0f, lastDistance = 0f, lastMaxSpeed = 0f, lastAvgSpeed = 0f;
    private long  lastElapsedMs = 0L;

    // Cronometro UI
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTimeMs = 0L;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTracking && tvTime != null) {
                tvTime.setText(formatTime(System.currentTimeMillis() - startTimeMs));
                timerHandler.postDelayed(this, 200);
            }
        }
    };

    // BroadcastReceiver — riceve dati GPS ogni ~1s dal SkiLocationService
    private final BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float   speed    = intent.getFloatExtra(SkiLocationService.EXTRA_SPEED_KMH, 0f);
            float   distance = intent.getFloatExtra(SkiLocationService.EXTRA_DISTANCE_KM, 0f);
            float   maxSpeed = intent.getFloatExtra(SkiLocationService.EXTRA_MAX_SPEED_KMH, 0f);
            float   avgSpeed = intent.getFloatExtra(SkiLocationService.EXTRA_AVG_SPEED_KMH, 0f);
            boolean onLift   = intent.getBooleanExtra(SkiLocationService.EXTRA_IS_RIDING_LIFT, false);

            if (tvSpeed == null) return;
            tvSpeed.setText(String.format("%.1f", speed));
            tvDistance.setText(String.format("%.2f", distance));
            tvMaxSpeed.setText(String.format("%.1f km/h", maxSpeed));
            tvAvgSpeed.setText(String.format("%.1f km/h", avgSpeed));
            tvChairlift.setVisibility(onLift ? View.VISIBLE : View.GONE);

            lastSpeed     = speed;
            lastDistance  = distance;
            lastMaxSpeed  = maxSpeed;
            lastAvgSpeed  = avgSpeed;
            lastElapsedMs = intent.getLongExtra(SkiLocationService.EXTRA_ELAPSED_MS, 0L);
        }
    };

    // Permission launchers — devono essere registrati PRIMA di onCreateView
    private final ActivityResultLauncher<String[]> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (Boolean.TRUE.equals(fine)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) requestBackgroundLocation();
                    else doStartTracking();
                } else {
                    Toast.makeText(requireContext(), "⚠️ Permesso GPS necessario", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> bgLocationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) Toast.makeText(requireContext(),
                        "⚠️ Senza 'Sempre' il tracking si ferma con lo schermo spento", Toast.LENGTH_LONG).show();
                doStartTracking();
            });

    // ---- Lifecycle ----

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSpeed      = view.findViewById(R.id.tv_speed);
        tvDistance   = view.findViewById(R.id.tv_distance);
        tvTime       = view.findViewById(R.id.tv_time);
        tvMaxSpeed   = view.findViewById(R.id.tv_max_speed);
        tvAvgSpeed   = view.findViewById(R.id.tv_avg_speed);
        tvChairlift  = view.findViewById(R.id.tv_chairlift_warning);
        btnStartStop = view.findViewById(R.id.btnStartStop);
        btnSos       = view.findViewById(R.id.btn_sos);

        btnStartStop.setOnClickListener(v -> {
            if (!isTracking) checkPermissionsAndStart();
            else             stopTracking();
        });

        // SOS: long-press per evitare pressioni accidentali
        btnSos.setOnLongClickListener(v -> { triggerEmergencySOS(); return true; });
        btnSos.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tieni premuto per inviare SOS", Toast.LENGTH_SHORT).show());

        // Richiedi permessi GPS all'apertura della tab
        requestPermissionsIfNeeded();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(trackingReceiver,
                        new IntentFilter(SkiLocationService.ACTION_TRACKING_UPDATE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(trackingReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }

    // ---- Permessi ----

    private void requestPermissionsIfNeeded() {
        boolean hasFine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasFine) {
            String[] perms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                   Manifest.permission.ACCESS_COARSE_LOCATION,
                                   Manifest.permission.POST_NOTIFICATIONS}
                    : new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                   Manifest.permission.ACCESS_COARSE_LOCATION};
            locationPermLauncher.launch(perms);
        }
    }

    private void checkPermissionsAndStart() {
        boolean hasFine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (hasFine) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) requestBackgroundLocation();
            else doStartTracking();
        } else {
            requestPermissionsIfNeeded();
        }
    }

    private void requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { doStartTracking(); return; }
        boolean hasBg = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (hasBg) { doStartTracking(); return; }

        new AlertDialog.Builder(requireContext())
                .setTitle("Tracciamento in background")
                .setMessage("Per continuare a registrare la discesa anche con lo schermo spento, "
                        + "concedi l'accesso alla posizione 'Sempre' nelle impostazioni.")
                .setPositiveButton("Concedi", (d, w) ->
                        bgLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                .setNegativeButton("Non ora", (d, w) -> doStartTracking())
                .show();
    }

    // ---- START / STOP ----

    private void doStartTracking() {
        isTracking  = true;
        startTimeMs = System.currentTimeMillis();

        btnStartStop.setText("STOP");
        btnStartStop.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFFF1744));

        Intent intent = new Intent(requireContext(), SkiLocationService.class);
        intent.setAction(SkiLocationService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            requireContext().startForegroundService(intent);
        else
            requireContext().startService(intent);

        timerHandler.post(timerRunnable);
        Toast.makeText(requireContext(), "🎿 Tracking avviato!", Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        isTracking = false;
        timerHandler.removeCallbacks(timerRunnable);

        btnStartStop.setText("START");
        btnStartStop.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF00E5FF));

        Intent intent = new Intent(requireContext(), SkiLocationService.class);
        intent.setAction(SkiLocationService.ACTION_STOP);
        requireContext().startService(intent);

        saveSessionToDatabase();
    }

    private void saveSessionToDatabase() {
        final SkiSession session = new SkiSession(
                System.currentTimeMillis(), lastElapsedMs, lastMaxSpeed, lastAvgSpeed, lastDistance,
                SkiLocationService.lastKnownLocation != null
                        ? (float) SkiLocationService.lastKnownLocation.getAltitude() : 0f);

        dbExecutor.execute(() -> {
            SkiSessionDao dao = AppDatabase.getInstance(requireContext()).skiSessionDao();
            dao.insert(session);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                String.format("✅ Sessione salvata! %.1f km · %.0f km/h max",
                                        lastDistance, lastMaxSpeed),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    // ---- S.O.S. ----

    private void triggerEmergencySOS() {
        android.location.Location loc = SkiLocationService.lastKnownLocation;
        String message;
        if (loc != null) {
            message = String.format(
                    "EMERGENZA: Ho bisogno di aiuto. " +
                    "Mi trovo a questa posizione: " +
                    "https://maps.google.com/?q=%.6f,%.6f " +
                    "- Altitudine: %.0fm.",
                    loc.getLatitude(), loc.getLongitude(), loc.getAltitude());
        } else {
            message = "EMERGENZA: Ho bisogno di aiuto sulle piste da sci. " +
                      "Posizione GPS non disponibile al momento.";
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Invia SOS tramite…"));
    }

    // ---- Utility ----

    private String formatTime(long ms) {
        long s = ms / 1000, h = s / 3600, m = (s % 3600) / 60;
        return String.format("%02d:%02d:%02d", h, m, s % 60);
    }
}
