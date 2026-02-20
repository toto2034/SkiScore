package it.unisa.skiscore.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import it.unisa.skiscore.R;

/**
 * Foreground Service che traccia il GPS per la sessione di sci.
 *
 * - FusedLocationProviderClient con aggiornamenti ogni 3s, PRIORITY_HIGH_ACCURACY.
 * - Calcola distanza, velocità e rileva seggiovia (ascesa consecutiva).
 * - Trasmette dati alla UI ogni secondo via LocalBroadcastManager.
 */
public class SkiLocationService extends Service {

    private static final String TAG = "SkiLocationService";

    // ---- Intent Actions ----
    public static final String ACTION_START = "START_TRACKING";
    public static final String ACTION_STOP  = "STOP_TRACKING";

    // ---- Broadcast constants ----
    public static final String ACTION_TRACKING_UPDATE = "it.unisa.skiscore.TRACKING_UPDATE";
    public static final String EXTRA_SPEED_KMH        = "speed_kmh";
    public static final String EXTRA_DISTANCE_KM      = "distance_km";
    public static final String EXTRA_ELAPSED_MS       = "elapsed_ms";
    public static final String EXTRA_MAX_SPEED_KMH    = "max_speed_kmh";
    public static final String EXTRA_AVG_SPEED_KMH    = "avg_speed_kmh";
    public static final String EXTRA_IS_RIDING_LIFT   = "is_riding_lift";
    public static final String EXTRA_ALTITUDE_M       = "altitude_m";

    // ---- Notification ----
    private static final String CHANNEL_ID    = "ski_tracking_channel";
    private static final int    NOTIFICATION_ID = 2001;

    // ---- GPS ----
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    // ---- Session state ----
    private boolean isTracking        = false;
    private long    startTimeMs       = 0L;
    private Location lastLocation     = null;

    // Stats
    private float totalDistanceMeters = 0f;
    private float maxSpeedKmh         = 0f;
    private float sumSpeedKmh         = 0f;
    private int   speedSampleCount    = 0;
    private float lastSpeedKmh        = 0f;

    // Chairlift / seggiovia detection
    private int     consecutiveAscentCount  = 0;
    private boolean isRidingLift            = false;
    private static final int   ASCENT_THRESHOLD_COUNT  = 3;    // # aggiornamenti di salita consecutivi
    private static final float ASCENT_MIN_DELTA_METERS = 2.0f; // min dislivello positivo per contare

    // ---- Broadcast timer (1 secondo) ----
    private final Handler broadcastHandler = new Handler(Looper.getMainLooper());
    private final Runnable broadcastRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTracking) {
                broadcastUpdate();
                broadcastHandler.postDelayed(this, 1000);
            }
        }
    };

    // -------- Lifecycle --------

    @Override
    public void onCreate() {
        super.onCreate();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        buildLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action)) startTracking();
        else if (ACTION_STOP.equals(action)) stopTracking();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Non usiamo Binder — tutto via LocalBroadcast
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedClient.removeLocationUpdates(locationCallback);
        broadcastHandler.removeCallbacks(broadcastRunnable);
    }

    // -------- Tracking control --------

    private void startTracking() {
        if (isTracking) return;
        isTracking             = true;
        startTimeMs            = System.currentTimeMillis();
        lastLocation           = null;
        totalDistanceMeters    = 0f;
        maxSpeedKmh            = 0f;
        sumSpeedKmh            = 0f;
        speedSampleCount       = 0;
        lastSpeedKmh           = 0f;
        consecutiveAscentCount = 0;
        isRidingLift           = false;

        startForeground(NOTIFICATION_ID, buildNotification());

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(2000L)
                .setMaxUpdateDelayMillis(5000L)
                .build();

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Permesso GPS mancante: " + e.getMessage());
            stopSelf();
            return;
        }

        // Avvia il timer broadcast ogni 1 secondo
        broadcastHandler.post(broadcastRunnable);
    }

    private void stopTracking() {
        isTracking = false;
        broadcastHandler.removeCallbacks(broadcastRunnable);
        fusedClient.removeLocationUpdates(locationCallback);
        // Ultimo broadcast finale
        broadcastUpdate();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    // -------- LocationCallback --------

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (!isTracking || result == null) return;
                Location location = result.getLastLocation();
                if (location != null) {
                    processLocation(location);
                }
            }
        };
    }

    /**
     * Elabora ogni fix GPS ricevuto.
     * Calcola velocità, distanza e rileva seggiovia.
     */
    private void processLocation(Location location) {

        // ---- 1. Rilevamento seggiovia (ascesa continua) ----
        if (lastLocation != null) {
            double altDelta = location.getAltitude() - lastLocation.getAltitude();
            if (altDelta > ASCENT_MIN_DELTA_METERS) {
                consecutiveAscentCount++;
            } else {
                // Ripristina se scende o rimane piatto
                consecutiveAscentCount = 0;
                isRidingLift = false;
            }
            if (consecutiveAscentCount >= ASCENT_THRESHOLD_COUNT) {
                isRidingLift = true;
            }
        }

        // ---- 2. Calcolo velocità istantanea ----
        float speedKmh = computeSpeedKmh(location);
        lastSpeedKmh = speedKmh;

        // ---- 3. Aggiornamento distanza e statistiche (solo se in discesa) ----
        if (!isRidingLift && lastLocation != null) {
            float deltaMeters = location.distanceTo(lastLocation);

            // Filtro anti-rumore: scarta salt GPS irrealistici
            boolean isValid = deltaMeters > 0.5f && speedKmh < 200f;

            if (isValid) {
                totalDistanceMeters += deltaMeters;

                // Aggiorna velocità media (solo > 2 km/h per escludere fermi)
                if (speedKmh > 2f) {
                    sumSpeedKmh += speedKmh;
                    speedSampleCount++;
                }

                // Velocità massima
                if (speedKmh > maxSpeedKmh) {
                    maxSpeedKmh = speedKmh;
                }
            }
        }

        lastLocation = location;
    }

    /**
     * Calcola la velocità in km/h.
     * Priorità: chip GPS (location.hasSpeed()) → derivata da spazio/tempo.
     */
    private float computeSpeedKmh(Location location) {
        // Preferisci il dato diretto dal chip GPS
        if (location.hasSpeed() && location.getSpeed() >= 0.3f) {
            return location.getSpeed() * 3.6f;
        }

        // Fallback: deriva da distanza / tempo trascorso
        if (lastLocation != null) {
            float distanceM  = location.distanceTo(lastLocation);
            long  timeDiffMs = location.getTime() - lastLocation.getTime();
            if (timeDiffMs > 0) {
                float speedMs = distanceM / (timeDiffMs / 1000f);
                return speedMs * 3.6f;
            }
        }

        return 0f;
    }

    /**
     * Calcola la velocità media solo delle discese (non seggiovia).
     */
    private float getAvgSpeedKmh() {
        return speedSampleCount > 0 ? sumSpeedKmh / speedSampleCount : 0f;
    }

    // -------- Broadcast --------

    /**
     * Invia un Intent locale con tutti i dati aggiornati di sessione.
     * Chiamato ogni secondo dal broadcastHandler.
     */
    private void broadcastUpdate() {
        long elapsedMs = System.currentTimeMillis() - startTimeMs;

        Intent intent = new Intent(ACTION_TRACKING_UPDATE);
        intent.putExtra(EXTRA_SPEED_KMH,      lastSpeedKmh);
        intent.putExtra(EXTRA_DISTANCE_KM,    totalDistanceMeters / 1000f);
        intent.putExtra(EXTRA_ELAPSED_MS,     elapsedMs);
        intent.putExtra(EXTRA_MAX_SPEED_KMH,  maxSpeedKmh);
        intent.putExtra(EXTRA_AVG_SPEED_KMH,  getAvgSpeedKmh());
        intent.putExtra(EXTRA_IS_RIDING_LIFT, isRidingLift);
        intent.putExtra(EXTRA_ALTITUDE_M,
                lastLocation != null ? (float) lastLocation.getAltitude() : 0f);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // -------- Notification --------

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "GPS Tracking", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Tracciamento GPS attivo per SkiScore");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, TrackerActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🎿 SkiScore — Tracking attivo")
                .setContentText("Registrazione discesa in corso…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }
}
