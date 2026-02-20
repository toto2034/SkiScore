package it.unisa.skiscore.tracker;

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
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;

import it.unisa.skiscore.R;

/**
 * TrackerActivity — gestisce START/STOP, permessi GPS, cronometro e aggiornamento UI.
 *
 * Flusso permessi:
 *   1. Richiede ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION
 *   2. Se Android 10+ (Q), richiede ACCESS_BACKGROUND_LOCATION come secondo step
 *      (Android richiede che sia in dialog separato, dopo la location di base)
 *
 * Comunicazione: BroadcastReceiver → LocalBroadcastManager (dal SkiLocationService).
 */
public class TrackerActivity extends AppCompatActivity {

    // ---- Views ----
    private TextView tvSpeed, tvDistance, tvTime, tvMaxSpeed, tvAvgSpeed, tvChairlift;
    private MaterialButton btnStartStop;
    private ImageButton btnBack;

    private boolean isTracking = false;

    // ---- Cronometro UI (indipendente dal GPS, 200ms) ----
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTimeMs = 0L;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTracking) {
                tvTime.setText(formatTime(System.currentTimeMillis() - startTimeMs));
                timerHandler.postDelayed(this, 200);
            }
        }
    };

    // -------- BroadcastReceiver --------

    /**
     * Riceve aggiornamenti GPS ogni ~1 secondo dal SkiLocationService.
     * Aggiorna tutte le TextView con valori formattati.
     */
    private final BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float   speed    = intent.getFloatExtra(SkiLocationService.EXTRA_SPEED_KMH, 0f);
            float   distance = intent.getFloatExtra(SkiLocationService.EXTRA_DISTANCE_KM, 0f);
            float   maxSpeed = intent.getFloatExtra(SkiLocationService.EXTRA_MAX_SPEED_KMH, 0f);
            float   avgSpeed = intent.getFloatExtra(SkiLocationService.EXTRA_AVG_SPEED_KMH, 0f);
            boolean onLift   = intent.getBooleanExtra(SkiLocationService.EXTRA_IS_RIDING_LIFT, false);

            // Formattazione leggibile stile cartello da pista
            tvSpeed.setText(String.format("%.1f", speed));        // "45.2"
            tvDistance.setText(String.format("%.2f", distance));   // "12.40"
            tvMaxSpeed.setText(String.format("%.1f km/h", maxSpeed));  // "78.3 km/h"
            tvAvgSpeed.setText(String.format("%.1f km/h", avgSpeed));  // "34.6 km/h"

            // Banner seggiovia
            tvChairlift.setVisibility(onLift ? View.VISIBLE : View.GONE);
        }
    };

    // -------- Permission launchers --------

    /** Step 1: chiede FINE + COARSE location */
    private final ActivityResultLauncher<String[]> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine   = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    // Step 2: su Android 10+ chiedi background location
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestBackgroundLocation();
                    } else {
                        doStartTracking();
                    }
                } else {
                    Toast.makeText(this, "⚠️ Permesso GPS necessario per il tracking", Toast.LENGTH_LONG).show();
                }
            });

    /** Step 2 (Android 10+): chiede ACCESS_BACKGROUND_LOCATION */
    private final ActivityResultLauncher<String> bgLocationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Il tracking funziona anche senza background location,
                // ma avvisiamo l'utente che lo schermo spento potrebbe non tracciare
                if (!granted) {
                    Toast.makeText(this,
                            "⚠️ Senza 'Sempre' il tracking si ferma con lo schermo spento",
                            Toast.LENGTH_LONG).show();
                }
                doStartTracking(); // Avvia comunque
            });

    // -------- Lifecycle --------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        tvSpeed      = findViewById(R.id.tv_speed);
        tvDistance   = findViewById(R.id.tv_distance);
        tvTime       = findViewById(R.id.tv_time);
        tvMaxSpeed   = findViewById(R.id.tv_max_speed);
        tvAvgSpeed   = findViewById(R.id.tv_avg_speed);
        tvChairlift  = findViewById(R.id.tv_chairlift_warning);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnBack      = findViewById(R.id.btn_back_tracker);

        btnBack.setOnClickListener(v -> finish());

        btnStartStop.setOnClickListener(v -> {
            if (!isTracking) checkPermissionsAndStart();
            else             stopTracking();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registra il receiver per aggiornamenti GPS dal Service
        IntentFilter filter = new IntentFilter(SkiLocationService.ACTION_TRACKING_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(trackingReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // De-registra il receiver (non servono update quando Activity è in background)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackingReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }

    // -------- Permessi --------

    private void checkPermissionsAndStart() {
        boolean hasFine   = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasFine || hasCoarse) {
            // Location di base già concessa — verifica background se Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocation();
            } else {
                doStartTracking();
            }
        } else {
            // Chiedi i permessi base
            String[] basePerms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS}
                    : new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};
            locationPermLauncher.launch(basePerms);
        }
    }

    private void requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            doStartTracking();
            return;
        }

        boolean hasBg = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasBg) {
            doStartTracking();
            return;
        }

        // Android richiede una rationale spiegata prima di presentare il dialog
        new AlertDialog.Builder(this)
                .setTitle("Tracciamento in background")
                .setMessage("Per continuare a registrare la discesa quando lo schermo è spento, "
                        + "concedi l'accesso alla posizione 'Sempre' nelle impostazioni.")
                .setPositiveButton("Concedi", (d, w) ->
                        bgLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                .setNegativeButton("Non ora", (d, w) ->
                        doStartTracking()) // Avvia lo stesso (tracking solo con schermo acceso)
                .show();
    }

    // -------- START / STOP --------

    private void doStartTracking() {
        isTracking  = true;
        startTimeMs = System.currentTimeMillis();

        // Aggiorna il pulsante → STOP rosso
        btnStartStop.setText("STOP");
        btnStartStop.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFFF1744));

        // Avvia il Foreground Service
        Intent intent = new Intent(this, SkiLocationService.class);
        intent.setAction(SkiLocationService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else                                                 startService(intent);

        // Avvia il cronometro UI
        timerHandler.post(timerRunnable);
        Toast.makeText(this, "🎿 Tracking avviato!", Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        isTracking = false;
        timerHandler.removeCallbacks(timerRunnable);

        // Ripristina il pulsante → START ciano
        btnStartStop.setText("START");
        btnStartStop.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF00E5FF));

        // Ferma il Service
        Intent intent = new Intent(this, SkiLocationService.class);
        intent.setAction(SkiLocationService.ACTION_STOP);
        startService(intent);
    }

    // -------- Utility --------

    /** Formatta millisecondi in HH:MM:SS — es. "01:23:45" */
    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
