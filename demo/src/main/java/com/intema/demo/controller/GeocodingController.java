package com.intema.demo.controller;

import com.intema.demo.service.GeocodingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/geocode")
public class GeocodingController {
    private final GeocodingService service;

    public GeocodingController(GeocodingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam String luogo) {
        try {
            return service.search(luogo)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Luogo non trovato sulla mappa"));
        } catch (GeocodingService.RateLimitedException ex) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Attendi un secondo prima di ripetere la ricerca");
        } catch (RestClientException | NumberFormatException ex) {
            return error(HttpStatus.BAD_GATEWAY, "Servizio di localizzazione temporaneamente non disponibile");
        }
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
