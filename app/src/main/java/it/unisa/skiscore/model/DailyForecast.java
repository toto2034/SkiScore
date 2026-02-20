package it.unisa.skiscore.model;

/**
 * Represents a single day's weather forecast, aggregated from OWM 3-hour slots.
 */
public class DailyForecast {
    private String date;
    private String dayName;
    private double tempMin;
    private double tempMax;
    private double snowfall;       // mm total
    private double windSpeedMax;   // m/s (max across 3h slots)
    private String weatherMain;    // Dominant condition
    private String weatherDescription;
    private String weatherIcon;

    public DailyForecast() {}

    public DailyForecast(String date, String dayName, double tempMin, double tempMax,
                         double snowfall, double windSpeedMax, String weatherMain,
                         String weatherDescription, String weatherIcon) {
        this.date = date;
        this.dayName = dayName;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.snowfall = snowfall;
        this.windSpeedMax = windSpeedMax;
        this.weatherMain = weatherMain;
        this.weatherDescription = weatherDescription;
        this.weatherIcon = weatherIcon;
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public double getTempMin() { return tempMin; }
    public void setTempMin(double tempMin) { this.tempMin = tempMin; }

    public double getTempMax() { return tempMax; }
    public void setTempMax(double tempMax) { this.tempMax = tempMax; }

    public double getSnowfall() { return snowfall; }
    public void setSnowfall(double snowfall) { this.snowfall = snowfall; }

    public double getWindSpeedMax() { return windSpeedMax; }
    public void setWindSpeedMax(double windSpeedMax) { this.windSpeedMax = windSpeedMax; }

    public String getWeatherMain() { return weatherMain; }
    public void setWeatherMain(String weatherMain) { this.weatherMain = weatherMain; }

    public String getWeatherDescription() { return weatherDescription; }
    public void setWeatherDescription(String weatherDescription) { this.weatherDescription = weatherDescription; }

    public String getWeatherIcon() { return weatherIcon; }
    public void setWeatherIcon(String weatherIcon) { this.weatherIcon = weatherIcon; }
}
