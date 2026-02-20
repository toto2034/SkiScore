package it.unisa.skiscore.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.unisa.skiscore.model.WebcamModel;
import it.unisa.skiscore.model.WebcamModel.StreamType;

/**
 * Static catalogue of webcam streams for known Italian ski resorts.
 *
 * Sources:
 *   • Feratel live streams (.mp4) for Livigno, Cortina, Corvara, Colfosco
 *   • YouTube live for Roccaraso, Ortisei
 *
 * Usage:
 *   List<WebcamModel> cams = WebcamRepository.getWebcamsForResort("Livigno");
 */
public class WebcamRepository {

    private static final Map<String, List<WebcamModel>> CATALOGUE = new HashMap<>();

    static {
        CATALOGUE.put("livigno", Arrays.asList(
                new WebcamModel(
                        "Livigno Live",
                        "https://sts004.feratel.co.at/streams/stsstore002/1/06172_69973f57-5a1cVid.mp4?dcsdesign=feratel4",
                        StreamType.VIDEO_MP4)
        ));

        CATALOGUE.put("roccaraso", Arrays.asList(
                new WebcamModel(
                        "Roccaraso Live",
                        "https://youtu.be/l9-M4s_7lKg",
                        StreamType.YOUTUBE)
        ));

        CATALOGUE.put("ortisei", Arrays.asList(
                new WebcamModel(
                        "Ortisei Live",
                        "https://youtu.be/_CAoxRHQsDA",
                        StreamType.YOUTUBE)
        ));

        CATALOGUE.put("cortina", Arrays.asList(
                new WebcamModel(
                        "Cortina d'Ampezzo Live",
                        "https://sts002.feratel.co.at/streams/stsstore001/1/06290_69973f1b-71f4Vid.mp4?dcsdesign=feratel4",
                        StreamType.VIDEO_MP4)
        ));

        CATALOGUE.put("corvara", Arrays.asList(
                new WebcamModel(
                        "Corvara Live",
                        "https://sts003.feratel.co.at/streams/stsstore001/1/06321_69973fbf-9e77Vid.mp4?dcsdesign=feratel4",
                        StreamType.VIDEO_MP4)
        ));

        CATALOGUE.put("colfosco", Arrays.asList(
                new WebcamModel(
                        "Colfosco Live",
                        "https://sts064.feratel.co.at/streams/stsstore053/1/06322_69973fce-ebdaVid.mp4?dcsdesign=feratel4",
                        StreamType.VIDEO_MP4)
        ));
    }

    /**
     * Returns webcams for a resort name. Matches by checking if the resort name
     * contains any known keyword (case-insensitive).
     *
     * @param resortName e.g. "Livigno", "Cortina d'Ampezzo"
     * @return list of WebcamModel, or empty list if no match
     */
    public static List<WebcamModel> getWebcamsForResort(String resortName) {
        if (resortName == null) return Collections.emptyList();
        String lower = resortName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<WebcamModel>> entry : CATALOGUE.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return Collections.emptyList();
    }
}
