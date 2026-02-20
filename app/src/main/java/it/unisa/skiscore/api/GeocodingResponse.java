package it.unisa.skiscore.api;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * POJO for Open-Meteo Geocoding API response.
 */
public class GeocodingResponse {

    @SerializedName("results")
    public List<GeoResult> results;

    public static class GeoResult {
        @SerializedName("id")
        public long id;

        @SerializedName("name")
        public String name;

        @SerializedName("latitude")
        public double latitude;

        @SerializedName("longitude")
        public double longitude;

        @SerializedName("country")
        public String country;

        @SerializedName("country_code")
        public String countryCode;

        @SerializedName("admin1")
        public String admin1; // Region/State

        @SerializedName("elevation")
        public double elevation;

        /**
         * Build a display string like "Cortina d'Ampezzo, Veneto, Italy"
         */
        public String getDisplayName() {
            StringBuilder sb = new StringBuilder(name);
            if (admin1 != null && !admin1.isEmpty()) {
                sb.append(", ").append(admin1);
            }
            if (country != null && !country.isEmpty()) {
                sb.append(", ").append(country);
            }
            return sb.toString();
        }
    }
}
