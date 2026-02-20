package it.unisa.skiscore.tracker;

import it.unisa.skiscore.db.AppDatabase;
import it.unisa.skiscore.db.SkiSession;
import it.unisa.skiscore.db.SkiSessionDao;

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
    private MaterialButton btnSos;
    private ImageButton btnBack;

    private boolean isTracking = false;

    /** Single-thread executor for Room operations (never run DB on Main Thread) */
    private final java.util.concurrent.ExecutorService dbExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    /** Last stats received from Service broadcast — used to persist on STOP */
    private float lastSpeed = 0f, lastDistance = 0f, lastMaxSpeed = 0f, lastAvgSpeed = 0f;
    private long  lastElapsedMs = 0L;

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

            // Cache per salvataggio su DB alla pressione di STOP
            lastSpeed     = speed;
            lastDistance  = distance;
            lastMaxSpeed  = maxSpeed;
            lastAvgSpeed  = avgSpeed;
            lastElapsedMs = intent.getLongExtra(SkiLocationService.EXTRA_ELAPSED_MS, 0L);
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
        btnSos       = findViewById(R.id.btn_sos);
        btnBack      = findViewById(R.id.btn_back_tracker);

        btnBack.setOnClickListener(v -> finish());

        // SOS: long-press per evitare pressioni accidentali in tasca
        btnSos.setOnLongClickListener(v -> {
            triggerEmergencySOS();
            return true;
        });
        // Short press: avvisa l'utente di usare il long-press
        btnSos.setOnClickListener(v ->
            Toast.makeText(this, "Tieni premuto per inviare SOS", Toast.LENGTH_SHORT).show()
        );

        btnStartStop.setOnClickListener(v -> {
            if (!isTracking) checkPermissionsAndStart();
            else             stopTracking();
        });

        // Richiedi i permessi GPS all'avvio, così sono pronti prima del tasto START
        requestPermissionsIfNeeded();
    }

    /**
     * Controlla e richiede i permessi necessari appena l'Activity si apre.
     * Non blocca l'accesso alla schermata — l'utente può vedere il layout
     * mentre il dialog di sistema compare.
     */
    private void requestPermissionsIfNeeded() {
        boolean hasFine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasFine) {
            String[] perms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS}
                    : new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};
            locationPermLauncher.launch(perms);
        }
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

        // Salva la sessione nel database Room in background
        saveSessionToDatabase();
    }

    /**
     * Crea una SkiSession con i dati dell'ultima discesa e la inserisce nel database
     * su un thread separato tramite ExecutorService (Room non consente operazioni sul Main Thread).
     */
    private void saveSessionToDatabase() {
        final SkiSession session = new SkiSession(
                System.currentTimeMillis(), // date: timestamp attuale
                lastElapsedMs,              // duration in ms
                lastMaxSpeed,               // km/h
                lastAvgSpeed,               // km/h
                lastDistance,               // km
                SkiLocationService.lastKnownLocation != null
                        ? (float) SkiLocationService.lastKnownLocation.getAltitude() : 0f
        );

        dbExecutor.execute(() -> {
            SkiSessionDao dao = AppDatabase.getInstance(TrackerActivity.this).skiSessionDao();
            dao.insert(session);
            // Feedback all'utente sul Main Thread
            runOnUiThread(() -> android.widget.Toast.makeText(
                    TrackerActivity.this,
                    String.format("✅ Sessione salvata! %.1f km · %.0f km/h max",
                            lastDistance, lastMaxSpeed),
                    android.widget.Toast.LENGTH_LONG
            ).show());
        });
    }

    // -------- S.O.S. --------

    /**
     * Recupera l'ultima posizione GPS nota e apre il selettore di condivisione Android
     * con un messaggio di emergenza precompilato contenente link Google Maps.
     */
    private void triggerEmergencySOS() {
        android.location.Location loc = SkiLocationService.lastKnownLocation;

        String message;
        if (loc != null) {
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            double alt = loc.getAltitude();
            message = String.format(
                    "EMERGENZA: Ho bisogno di aiuto. " +
                    "Mi trovo a questa posizione: " +
                    "https://maps.google.com/?q=%.6f,%.6f " +
                    "- Altitudine: %.0fm.",
                    lat, lon, alt);
        } else {
            // Posizione non ancora disponibile
            message = "EMERGENZA: Ho bisogno di aiuto sulle piste da sci. " +
                      "Posizione GPS non disponibile al momento.";
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        // Apre il selettore (WhatsApp, SMS, Telegram, ecc.)
        startActivity(Intent.createChooser(shareIntent, "Invia SOS tramite…"));
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
