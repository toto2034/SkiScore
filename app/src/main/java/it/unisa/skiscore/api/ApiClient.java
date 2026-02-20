package it.unisa.skiscore.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Singleton Retrofit client builder for OpenWeatherMap and Open-Meteo Geocoding APIs.
 */
public class ApiClient {

    private static final String OWM_BASE_URL = "https://api.openweathermap.org";
    private static final String GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com";

    /**
     * ⚠️ INSERISCI QUI LA TUA API KEY di OpenWeatherMap.
     * Registrati gratuitamente su https://openweathermap.org/api
     * e ottieni una chiave per il piano "Current Weather" + "5 Day Forecast".
     */
    public static final String OWM_API_KEY = "6e7eeb99b4719e39c85ab37a7768c824";

    private static OwmWeatherService weatherService;
    private static OwmForecastService forecastService;
    private static GeocodingService geocodingService;

    private static OkHttpClient getHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
    }

    public static synchronized OwmWeatherService getWeatherService() {
        if (weatherService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(OWM_BASE_URL)
                    .client(getHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            weatherService = retrofit.create(OwmWeatherService.class);
        }
        return weatherService;
    }

    public static synchronized OwmForecastService getForecastService() {
        if (forecastService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(OWM_BASE_URL)
                    .client(getHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            forecastService = retrofit.create(OwmForecastService.class);
        }
        return forecastService;
    }

    public static synchronized GeocodingService getGeocodingService() {
        if (geocodingService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(GEOCODING_BASE_URL)
                    .client(getHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            geocodingService = retrofit.create(GeocodingService.class);
        }
        return geocodingService;
    }
}
