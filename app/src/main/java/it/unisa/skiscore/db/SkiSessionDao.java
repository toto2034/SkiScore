package it.unisa.skiscore.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for ski sessions.
 * All query methods must be called from a background thread (Room enforces this).
 */
@Dao
public interface SkiSessionDao {

    /** Insert a new session. Replaces on conflict (shouldn't happen with autoGenerate PK). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SkiSession session);

    /** Returns all sessions ordered by date descending, as LiveData for reactive UI. */
    @Query("SELECT * FROM ski_sessions ORDER BY date DESC")
    LiveData<List<SkiSession>> getAllSessions();

    // ---- Aggregate stats for season summary ----

    /** Total km skied across all sessions. */
    @Query("SELECT COALESCE(SUM(totalDistance), 0) FROM ski_sessions")
    float getTotalDistance();

    /** Best single-session top speed ever recorded. */
    @Query("SELECT COALESCE(MAX(maxSpeed), 0) FROM ski_sessions")
    float getMaxSpeedSeason();

    /**
     * Number of distinct days with at least one recorded session.
     * Uses date/86400000 to truncate timestamp to day granularity.
     */
    @Query("SELECT COUNT(DISTINCT date / 86400000) FROM ski_sessions")
    int getTotalDaysSkied();
}
