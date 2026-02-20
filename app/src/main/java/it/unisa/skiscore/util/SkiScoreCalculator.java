package it.unisa.skiscore.util;

import it.unisa.skiscore.model.WeatherData;

/**
 * Calculates the Skiability Score (0-100%) based on OpenWeatherMap data.
 *
 * Weight distribution:
 *  - Precipitazioni e Neve: 30%
 *  - Visibilità e Cielo:    30%
 *  - Vento:                  25%
 *  - Temperatura:            15%
 */
public class SkiScoreCalculator {

    // Weights (sum = 1.0)
    private static final double WEIGHT_SNOW = 0.30;
    private static final double WEIGHT_VISIBILITY = 0.30;
    private static final double WEIGHT_WIND = 0.25;
    private static final double WEIGHT_TEMPERATURE = 0.15;

    /**
     * Calcola lo Skiability Score dai dati OWM.
     *
     * @param data WeatherData mappato dalla risposta OWM
     * @return punteggio intero 0-100
     */
    public static int calculateSkiScore(WeatherData data) {
        if (data == null) return 0;

        double snowScore = calculateSnowScore(data.getWeatherMain(), data.getSnowfall1h(), data.getSnowfall3h());
        double visibilityScore = calculateVisibilityScore(data.getWeatherMain(), data.getVisibility());
        double windScore = calculateWindScore(data.getWindSpeed());
        double tempScore = calculateTemperatureScore(data.getTempCurrent());

        double totalScore = (snowScore * WEIGHT_SNOW)
                + (visibilityScore * WEIGHT_VISIBILITY)
                + (windScore * WEIGHT_WIND)
                + (tempScore * WEIGHT_TEMPERATURE);

        return (int) Math.round(Math.max(0, Math.min(100, totalScore)));
    }

    /**
     * Precipitazioni e Neve (0-100).
     *
     * - weather[0].main == "Snow" con volume 2-5 mm/h → 100 (neve fresca ideale)
     * - weather[0].main == "Snow" con volume > 5 mm/h → 70 (troppa neve, visibilità ridotta)
     * - weather[0].main == "Snow" con volume < 2 mm/h → 80 (leggera spolverata)
     * - weather[0].main == "Snow" senza dati snow → 60 (neve ma quantità sconosciuta)
     * - Nessuna precipitazione → 40 (niente neve fresca ma ok se c'è base)
     * - weather[0].main == "Rain" → 0 (pioggia sulle piste!)
     * - weather[0].main == "Drizzle" → 10
     */
    public static double calculateSnowScore(String weatherMain, double snow1h, double snow3h) {
        if (weatherMain == null) return 40;

        switch (weatherMain) {
            case "Snow":
                // Usa snow.1h se disponibile, altrimenti stima da snow.3h
                double snowRate = snow1h > 0 ? snow1h : (snow3h > 0 ? snow3h / 3.0 : 0);

                if (snowRate <= 0) return 60;          // Neve ma senza dati precisi
                if (snowRate >= 2 && snowRate <= 5) return 100; // Neve fresca ideale
                if (snowRate < 2) return 80;           // Spolverata leggera
                if (snowRate <= 8) return 70;          // Nevicata abbondante
                return 50;                              // Bufera

            case "Rain":
                return 0;   // Pioggia sulle piste: disastroso!

            case "Drizzle":
                return 10;  // Pioviggine: pessimo

            case "Thunderstorm":
                return 0;   // Temporale: pericoloso

            default:
                return 40;  // Nessuna precipitazione neve fresca, ma OK
        }
    }

    /**
     * Visibilità e Cielo (0-100) basata su weather[0].main e visibility (metri).
     *
     * - "Clear" → 100
     * - "Clouds" → 60
     * - "Mist" o "Fog" → 10
     * - visibility < 1000m → 10 (indipendentemente dal main)
     * - "Snow" → 50 (neve riduce visibilità ma è positivo per lo sci)
     * - "Haze" → 30
     */
    public static double calculateVisibilityScore(String weatherMain, int visibilityMeters) {
        // Visibilità molto bassa override tutto
        if (visibilityMeters > 0 && visibilityMeters < 1000) {
            return 10;
        }

        if (weatherMain == null) return 50;

        double baseScore;
        switch (weatherMain) {
            case "Clear":
                baseScore = 100;
                break;
            case "Clouds":
                baseScore = 60;
                break;
            case "Snow":
                baseScore = 50; // Neve riduce visibilità ma buona per sci
                break;
            case "Haze":
                baseScore = 30;
                break;
            case "Mist":
            case "Fog":
                baseScore = 10;
                break;
            case "Rain":
            case "Drizzle":
                baseScore = 20;
                break;
            case "Thunderstorm":
                baseScore = 5;
                break;
            default:
                baseScore = 40;
                break;
        }

        // Penalizza ulteriormente se visibility è bassa (1000-3000m)
        if (visibilityMeters > 0 && visibilityMeters < 3000) {
            baseScore = Math.min(baseScore, 30);
        }

        return baseScore;
    }

    /**
     * Vento (0-100) basato su wind.speed in m/s.
     *
     * - < 3 m/s (≈10 km/h) → 100 (calmo, perfetto)
     * - 3-7 m/s → interpolazione lineare 100-60 (moderato)
     * - 7-12 m/s → interpolazione lineare 60-0 (forte, rischio)
     * - > 12 m/s → 0 (impianti chiusi!)
     */
    public static double calculateWindScore(double windSpeedMs) {
        if (windSpeedMs < 3) {
            return 100;
        } else if (windSpeedMs <= 7) {
            return 100 - (windSpeedMs - 3) * (40.0 / 4.0);
        } else if (windSpeedMs <= 12) {
            return 60 - (windSpeedMs - 7) * (60.0 / 5.0);
        } else {
            return 0;
        }
    }

    /**
     * Temperatura (0-100) basata su main.temp in °C.
     *
     * Sweet spot: -8°C a +1°C → 100
     * Freddo: da -8°C a -15°C → diminuisce a 30
     * Molto freddo: < -15°C → 30
     * Caldo: da +1°C a +5°C → diminuisce a 40
     * Troppo caldo: > +5°C → diminuisce a 0 (neve pappa)
     */
    public static double calculateTemperatureScore(double tempCelsius) {
        if (tempCelsius >= -8 && tempCelsius <= 1) {
            return 100;
        } else if (tempCelsius < -8 && tempCelsius >= -15) {
            return 100 - (-8 - tempCelsius) * (70.0 / 7.0);
        } else if (tempCelsius < -15) {
            return 30;
        } else if (tempCelsius > 1 && tempCelsius <= 5) {
            return 100 - (tempCelsius - 1) * (60.0 / 4.0);
        } else if (tempCelsius > 5 && tempCelsius <= 15) {
            return 40 - (tempCelsius - 5) * (40.0 / 10.0);
        } else {
            return 0;
        }
    }

    /**
     * Categoria di colore per il punteggio.
     * @return "red" < 40, "yellow" 40-70, "green" > 70
     */
    public static String getScoreCategory(int score) {
        if (score >= 70) return "green";
        if (score >= 40) return "yellow";
        return "red";
    }

    /**
     * Etichetta testuale per il punteggio.
     */
    public static String getScoreLabel(int score) {
        if (score >= 80) return "Eccellente";
        if (score >= 60) return "Buono";
        if (score >= 40) return "Discreto";
        return "Scarso";
    }
}
