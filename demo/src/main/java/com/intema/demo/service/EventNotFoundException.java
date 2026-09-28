package com.intema.demo.service;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(Long id) {
        super("Evento " + id + " non trovato");
    }
}
