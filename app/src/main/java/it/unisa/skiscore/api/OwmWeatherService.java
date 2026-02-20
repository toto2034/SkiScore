package it.unisa.skiscore.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service for OpenWeatherMap Current Weather Data API.
 * Docs: https://openweathermap.org/current
 */
public interface OwmWeatherService {

    @GET("/data/2.5/weather")
    Call<OwmCurrentResponse> getCurrentWeather(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("appid") String apiKey,
            @Query("units") String units,     // "metric" for °C, m/s
            @Query("lang") String language    // "it" for Italian descriptions
    );
}
