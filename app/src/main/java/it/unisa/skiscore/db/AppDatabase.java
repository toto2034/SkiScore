package it.unisa.skiscore.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Singleton Room database for SkiScore.
 *
 * Usage:
 *   AppDatabase db = AppDatabase.getInstance(context);
 *   SkiSessionDao dao = db.skiSessionDao();
 */
@Database(entities = {SkiSession.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SkiSessionDao skiSessionDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "skiscore.db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
