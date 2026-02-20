package it.unisa.skiscore.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * POJO for OpenWeatherMap Current Weather API response.
 * Docs: https://openweathermap.org/current#fields_json
 *
 * Example endpoint: /data/2.5/weather?lat=46.5&lon=11.3&appid=KEY&units=metric
 */
public class OwmCurrentResponse {

    @SerializedName("weather")
    public List<Weather> weather;

    @SerializedName("main")
    public Main main;

    @SerializedName("wind")
    public Wind wind;

    @SerializedName("visibility")
    public int visibility;  // meters

    @SerializedName("snow")
    public Snow snow;

    @SerializedName("rain")
    public Rain rain;

    @SerializedName("name")
    public String cityName;

    @SerializedName("dt")
    public long dt;

    // --- Nested classes matching OWM JSON ---

    public static class Weather {
        @SerializedName("id")
        public int id;

        @SerializedName("main")
        public String main;    // "Clear", "Clouds", "Snow", "Rain", "Mist", "Fog", etc.

        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;
    }

    public static class Main {
        @SerializedName("temp")
        public double temp;         // °C (with units=metric)

        @SerializedName("feels_like")
        public double feelsLike;

        @SerializedName("temp_min")
        public double tempMin;

        @SerializedName("temp_max")
        public double tempMax;

        @SerializedName("pressure")
        public int pressure;

        @SerializedName("humidity")
        public int humidity;
    }

    public static class Wind {
        @SerializedName("speed")
        public double speed;    // m/s (with units=metric)

        @SerializedName("gust")
        public double gust;     // m/s

        @SerializedName("deg")
        public int deg;
    }

    public static class Snow {
        @SerializedName("1h")
        public double oneHour;  // mm in last 1h

        @SerializedName("3h")
        public double threeHour; // mm in last 3h
    }

    public static class Rain {
        @SerializedName("1h")
        public double oneHour;

        @SerializedName("3h")
        public double threeHour;
    }
}
