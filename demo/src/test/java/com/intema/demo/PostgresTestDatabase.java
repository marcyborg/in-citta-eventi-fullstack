package com.intema.demo;

public final class PostgresTestDatabase {
    private PostgresTestDatabase() {}

    public static String url() {
        String url = required("POSTGRES_IT_URL");
        if (!url.matches("jdbc:postgresql://[^/]+/in_citta_test(?:\\?.*)?")) {
            throw new IllegalArgumentException("I test PostgreSQL richiedono il database dedicato in_citta_test");
        }
        return url;
    }

    public static String user() { return required("POSTGRES_IT_USER"); }
    public static String password() { return required("POSTGRES_IT_PASSWORD"); }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Variabile di test mancante: " + name);
        return value;
    }
}
