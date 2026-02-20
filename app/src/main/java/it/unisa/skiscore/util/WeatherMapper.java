package it.unisa.skiscore.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.unisa.skiscore.api.OwmCurrentResponse;
import it.unisa.skiscore.api.OwmForecastResponse;
import it.unisa.skiscore.model.DailyForecast;
import it.unisa.skiscore.model.WeatherData;

/**
 * Static helpers to map OpenWeatherMap API responses to our internal model.
 * Extracted from MainActivity so multiple screens can reuse them.
 */
public class WeatherMapper {

    private WeatherMapper() { /* utility class */ }

    /**
     * Maps an OWM Current Weather response to our internal {@link WeatherData}.
     */
    public static WeatherData mapOwmCurrentToWeatherData(OwmCurrentResponse r) {
        WeatherData data = new WeatherData();

        if (r.main != null) {
            data.setTempCurrent(r.main.temp);
            data.setFeelsLike(r.main.feelsLike);
            data.setTempMin(r.main.tempMin);
            data.setTempMax(r.main.tempMax);
        }
        if (r.wind != null) {
            data.setWindSpeed(r.wind.speed);
        }
        if (r.snow != null) {
            data.setSnowfall1h(r.snow.oneHour);
            data.setSnowfall3h(r.snow.threeHour);
        }
        data.setVisibility(r.visibility);
        if (r.weather != null && !r.weather.isEmpty()) {
            data.setWeatherMain(r.weather.get(0).main);
            data.setWeatherDescription(r.weather.get(0).description);
        }
        return data;
    }

    /**
     * Aggregates a 5-day/3-hour OWM forecast into one {@link DailyForecast} per day.
     * Groups slots by date string (first 10 chars of dt_txt, e.g. "2025-02-20").
     */
    public static List<DailyForecast> aggregateForecast(OwmForecastResponse r) {
        Map<String, List<OwmForecastResponse.ForecastItem>> byDay = new LinkedHashMap<>();
        if (r.list == null) return new ArrayList<>();

        for (OwmForecastResponse.ForecastItem item : r.list) {
            String day = item.dtTxt != null ? item.dtTxt.substring(0, 10) : "unknown";
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(item);
        }

        List<DailyForecast> result = new ArrayList<>();
        for (Map.Entry<String, List<OwmForecastResponse.ForecastItem>> entry : byDay.entrySet()) {
            List<OwmForecastResponse.ForecastItem> slots = entry.getValue();
            double minTemp = Double.MAX_VALUE, maxTemp = -Double.MAX_VALUE;
            double snowSum = 0, windMax = 0;
            String weatherMain = "", weatherDesc = "";

            for (OwmForecastResponse.ForecastItem slot : slots) {
                if (slot.main != null) {
                    if (slot.main.tempMin < minTemp) minTemp = slot.main.tempMin;
                    if (slot.main.tempMax > maxTemp) maxTemp = slot.main.tempMax;
                }
                if (slot.snow != null) snowSum += slot.snow.threeHour;
                if (slot.wind != null && slot.wind.speed > windMax) windMax = slot.wind.speed;
                if (slot.weather != null && !slot.weather.isEmpty() && weatherMain.isEmpty()) {
                    weatherMain = slot.weather.get(0).main;
                    weatherDesc = slot.weather.get(0).description;
                }
            }

            DailyForecast df = new DailyForecast();
            df.setDate(entry.getKey());
            df.setTempMin(minTemp == Double.MAX_VALUE ? 0 : minTemp);
            df.setTempMax(maxTemp == -Double.MAX_VALUE ? 0 : maxTemp);
            df.setSnowfall(snowSum);
            df.setWindSpeedMax(windMax);
            df.setWeatherMain(weatherMain);
            df.setWeatherDescription(weatherDesc);
            result.add(df);
        }
        return result;
    }
}
