package it.unisa.skiscore.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service interface for Open-Meteo Geocoding API.
 * Base URL: https://geocoding-api.open-meteo.com
 */
public interface GeocodingService {

    @GET("/v1/search")
    Call<GeocodingResponse> searchLocation(
            @Query("name") String name,
            @Query("count") int count,
            @Query("language") String language,
            @Query("format") String format
    );
}
