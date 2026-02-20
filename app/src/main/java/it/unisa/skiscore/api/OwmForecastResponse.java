package it.unisa.skiscore.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * POJO for OpenWeatherMap 5 Day / 3 Hour Forecast API response.
 * Docs: https://openweathermap.org/forecast5
 *
 * The response contains a list of 40 forecasts (5 days × 8 per day, every 3 hours).
 */
public class OwmForecastResponse {

    @SerializedName("list")
    public List<ForecastItem> list;

    @SerializedName("city")
    public City city;

    public static class ForecastItem {
        @SerializedName("dt")
        public long dt;

        @SerializedName("main")
        public OwmCurrentResponse.Main main;

        @SerializedName("weather")
        public List<OwmCurrentResponse.Weather> weather;

        @SerializedName("wind")
        public OwmCurrentResponse.Wind wind;

        @SerializedName("visibility")
        public int visibility;

        @SerializedName("snow")
        public OwmCurrentResponse.Snow snow;

        @SerializedName("rain")
        public OwmCurrentResponse.Rain rain;

        @SerializedName("dt_txt")
        public String dtTxt; // "2025-02-20 12:00:00"
    }

    public static class City {
        @SerializedName("name")
        public String name;

        @SerializedName("country")
        public String country;

        @SerializedName("coord")
        public Coord coord;
    }

    public static class Coord {
        @SerializedName("lat")
        public double lat;

        @SerializedName("lon")
        public double lon;
    }
}
