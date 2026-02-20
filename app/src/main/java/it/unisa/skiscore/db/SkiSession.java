package it.unisa.skiscore.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a single ski session saved to the local Room database.
 */
@Entity(tableName = "ski_sessions")
public class SkiSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Timestamp (ms since epoch) of when the session was recorded */
    public long date;

    /** Duration of the session in milliseconds */
    public long duration;

    /** Maximum speed reached during the session (km/h) */
    public float maxSpeed;

    /** Average speed during descent (chairlift excluded) (km/h) */
    public float avgSpeed;

    /** Total distance covered during the session (km) */
    public float totalDistance;

    /** Total vertical drop (negative elevation change) in meters */
    public float verticalDrop;

    public SkiSession() {}

    public SkiSession(long date, long duration, float maxSpeed,
                      float avgSpeed, float totalDistance, float verticalDrop) {
        this.date          = date;
        this.duration      = duration;
        this.maxSpeed      = maxSpeed;
        this.avgSpeed      = avgSpeed;
        this.totalDistance = totalDistance;
        this.verticalDrop  = verticalDrop;
    }
}
