package com.intema.demo.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GeocodingServiceTest {
    @Test
    void geocodingCachesResultsAndLimitsOutboundTraffic() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", exchange -> {
            requests.incrementAndGet();
            assertTrue(exchange.getRequestHeaders().getFirst("User-Agent").contains("InCittaEventiMappa"));
            byte[] json = "[{\"lat\":\"45.4642\",\"lon\":\"9.1900\",\"display_name\":\"Milano, Italia\"}]"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length);
            try (var body = exchange.getResponseBody()) {
                body.write(json);
            }
        });
        server.start();
        try {
            GeocodingService service = new GeocodingService(
                    "http://127.0.0.1:" + server.getAddress().getPort());
            var first = service.search("Milano");
            assertEquals(45.4642, first.orElseThrow().latitude(), 0.0001);
            assertEquals(first, service.search(" MILANO "));
            assertEquals(1, requests.get());
            assertThrows(GeocodingService.RateLimitedException.class,
                    () -> service.search("Torino"));
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }
}
