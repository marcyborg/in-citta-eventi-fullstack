package com.intema.demo.service;

import com.intema.demo.model.Event;
import com.intema.demo.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {
    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public List<Event> getAllEvents() {
        return repository.findAll(Sort.by("data").ascending());
    }

    public Event getEvent(Long id) {
        return repository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    public Event saveEvent(Event event) {
        event.setId(null);
        return repository.save(event);
    }

    public Event updateEvent(Long id, Event event) {
        getEvent(id);
        event.setId(id);
        return repository.save(event);
    }

    public void deleteEvent(Long id) {
        getEvent(id);
        repository.deleteById(id);
    }

    public List<Event> byCategoria(String categoria) {
        return repository.findByCategoriaIgnoreCaseOrderByDataAsc(categoria);
    }

    public List<Event> byDate(LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        return repository.findByDataBetweenOrderByDataAsc(start, end);
    }

    public List<Event> upcomingEvents() {
        return repository.findByDataGreaterThanEqualOrderByDataAsc(LocalDateTime.now());
    }

    public Page<Event> search(String categoria, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        validateRange(start, end);
        return repository.search(categoria == null || categoria.isBlank() ? null : categoria.trim(),
                start, end, pageable);
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("La data iniziale deve precedere quella finale");
        }
    }
}
