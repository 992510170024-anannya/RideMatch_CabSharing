package com.ridematch.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** Normalizes user-entered location names without geocoding or external APIs. */
@Component
public class LocationNormalizer {

    public String normalize(String location) {
        if (location == null) {
            return "";
        }
        String withoutDiacritics = Normalizer.normalize(location, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutDiacritics.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
