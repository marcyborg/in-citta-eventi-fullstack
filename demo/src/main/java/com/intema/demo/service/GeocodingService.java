package com.intema.demo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeocodingService {
    public record Location(double latitude, double longitude, String displayName) {}
    private record NominatimPlace(String lat, String lon,
                                  @JsonProperty("display_name") String displayName) {}

    private final RestClient client;
    private final Map<String, Optional<Location>> cache = new ConcurrentHashMap<>();
    private long nextAllowedAt = 0;

    public GeocodingService(@Value("${app.geocoding.base-url}") String baseUrl) {
        client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "InCittaEventiMappa/1.0")
                .build();
    }

    // A single instance serializes outbound searches and caches repeated queries.
    // Search is only invoked by the explicit "Localizza" button; never autocomplete.
    public synchronized Optional<Location> search(String place) {
        String key = place.strip().toLowerCase(java.util.Locale.ROOT);
        if (key.isBlank() || key.length() > 255) {
            throw new IllegalArgumentException("Il luogo deve contenere da 1 a 255 caratteri");
        }
        if (cache.containsKey(key)) return cache.get(key);
        long now = System.currentTimeMillis();
        if (now < nextAllowedAt) throw new RateLimitedException();
        nextAllowedAt = now + 1100;

        NominatimPlace[] found = client.get()
                .uri(builder -> builder.path("/search")
                        .queryParam("q", place.strip())
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", 1)
                        .build())
                .retrieve().body(NominatimPlace[].class);
        Optional<Location> result = found == null || found.length == 0
                ? Optional.empty()
                : Optional.of(new Location(Double.parseDouble(found[0].lat()),
                        Double.parseDouble(found[0].lon()), found[0].displayName()));
        cache.put(key, result);
        return result;
    }

    public static class RateLimitedException extends RuntimeException {}
}
