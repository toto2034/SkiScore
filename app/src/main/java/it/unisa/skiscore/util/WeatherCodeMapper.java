package it.unisa.skiscore.util;

/**
 * Maps OpenWeatherMap weather[0].main strings to Italian condition text and emoji.
 * Reference: https://openweathermap.org/weather-conditions
 */
public class WeatherCodeMapper {

    /**
     * Get a human-readable Italian weather condition text from OWM weather.main.
     */
    public static String getConditionText(String weatherMain) {
        if (weatherMain == null) return "Sconosciuto";

        switch (weatherMain) {
            case "Clear":         return "Cielo sereno";
            case "Clouds":        return "Nuvoloso";
            case "Snow":          return "Neve";
            case "Rain":          return "Pioggia";
            case "Drizzle":       return "Pioviggine";
            case "Thunderstorm":  return "Temporale";
            case "Mist":          return "Foschia";
            case "Fog":           return "Nebbia";
            case "Haze":          return "Caligine";
            case "Smoke":         return "Fumo";
            case "Dust":          return "Polvere";
            case "Sand":          return "Sabbia";
            case "Ash":           return "Cenere";
            case "Squall":        return "Burrasca";
            case "Tornado":       return "Tornado";
            default:              return weatherMain;
        }
    }

    /**
     * Get a weather emoji from OWM weather.main.
     */
    public static String getWeatherEmoji(String weatherMain) {
        if (weatherMain == null) return "🌡️";

        switch (weatherMain) {
            case "Clear":         return "☀️";
            case "Clouds":        return "☁️";
            case "Snow":          return "🌨️";
            case "Rain":          return "🌧️";
            case "Drizzle":       return "🌧️";
            case "Thunderstorm":  return "⛈️";
            case "Mist":          return "🌫️";
            case "Fog":           return "🌫️";
            case "Haze":          return "🌫️";
            case "Squall":        return "💨";
            default:              return "🌡️";
        }
    }

    /**
     * Get OWM icon URL for displaying weather images.
     * @param iconCode e.g. "10d", "13n"
     * @return URL to the 2x icon PNG
     */
    public static String getIconUrl(String iconCode) {
        if (iconCode == null || iconCode.isEmpty()) return "";
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }
}
