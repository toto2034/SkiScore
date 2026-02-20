package it.unisa.skiscore.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service for OpenWeatherMap 5 Day / 3 Hour Forecast API.
 * Docs: https://openweathermap.org/forecast5
 */
public interface OwmForecastService {

    @GET("/data/2.5/forecast")
    Call<OwmForecastResponse> getForecast(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String language
    );
}
