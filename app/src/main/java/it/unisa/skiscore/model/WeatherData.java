package it.unisa.skiscore.model;

import java.util.List;

/**
 * Represents the current weather data for a ski resort location.
 * Fields are mapped from OpenWeatherMap API response.
 */
public class WeatherData {
    private double tempCurrent;     // °C (main.temp)
    private double tempMin;         // °C (main.temp_min)
    private double tempMax;         // °C (main.temp_max)
    private double feelsLike;       // °C (main.feels_like)
    private double windSpeed;       // m/s (wind.speed)
    private double windGust;        // m/s (wind.gust)
    private double snowfall1h;      // mm (snow.1h, 0 if absent)
    private double snowfall3h;      // mm (snow.3h, 0 if absent)
    private int visibility;         // meters (visibility)
    private String weatherMain;     // "Clear", "Clouds", "Snow", "Rain", etc. (weather[0].main)
    private String weatherDescription; // Full description (weather[0].description)
    private String weatherIcon;     // Icon code (weather[0].icon)
    private int weatherId;          // OWM weather condition id
    private int humidity;           // % (main.humidity)
    private List<DailyForecast> forecast;

    public WeatherData() {}

    // Getters and Setters
    public double getTempCurrent() { return tempCurrent; }
    public void setTempCurrent(double tempCurrent) { this.tempCurrent = tempCurrent; }

    public double getTempMin() { return tempMin; }
    public void setTempMin(double tempMin) { this.tempMin = tempMin; }

    public double getTempMax() { return tempMax; }
    public void setTempMax(double tempMax) { this.tempMax = tempMax; }

    public double getFeelsLike() { return feelsLike; }
    public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public double getWindGust() { return windGust; }
    public void setWindGust(double windGust) { this.windGust = windGust; }

    public double getSnowfall1h() { return snowfall1h; }
    public void setSnowfall1h(double snowfall1h) { this.snowfall1h = snowfall1h; }

    public double getSnowfall3h() { return snowfall3h; }
    public void setSnowfall3h(double snowfall3h) { this.snowfall3h = snowfall3h; }

    public int getVisibility() { return visibility; }
    public void setVisibility(int visibility) { this.visibility = visibility; }

    public String getWeatherMain() { return weatherMain; }
    public void setWeatherMain(String weatherMain) { this.weatherMain = weatherMain; }

    public String getWeatherDescription() { return weatherDescription; }
    public void setWeatherDescription(String weatherDescription) { this.weatherDescription = weatherDescription; }

    public String getWeatherIcon() { return weatherIcon; }
    public void setWeatherIcon(String weatherIcon) { this.weatherIcon = weatherIcon; }

    public int getWeatherId() { return weatherId; }
    public void setWeatherId(int weatherId) { this.weatherId = weatherId; }

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public List<DailyForecast> getForecast() { return forecast; }
    public void setForecast(List<DailyForecast> forecast) { this.forecast = forecast; }
}
