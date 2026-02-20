package it.unisa.skiscore.model;

import java.io.Serializable;

/**
 * Represents a ski resort with its location, weather data, and computed ski score.
 */
public class SkiResort implements Serializable {
    private String name;
    private String country;
    private double latitude;
    private double longitude;
    private boolean isFavorite;
    private transient WeatherData weatherData;
    private int skiScore;

    public SkiResort() {}

    public SkiResort(String name, String country, double latitude, double longitude) {
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFavorite = false;
        this.skiScore = -1; // not yet calculated
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public WeatherData getWeatherData() { return weatherData; }
    public void setWeatherData(WeatherData weatherData) { this.weatherData = weatherData; }

    public int getSkiScore() { return skiScore; }
    public void setSkiScore(int skiScore) { this.skiScore = skiScore; }

    /**
     * Unique key for favorites storage.
     */
    public String getKey() {
        return name + "_" + latitude + "_" + longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkiResort that = (SkiResort) o;
        return Double.compare(that.latitude, latitude) == 0
                && Double.compare(that.longitude, longitude) == 0
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        return result;
    }
}
