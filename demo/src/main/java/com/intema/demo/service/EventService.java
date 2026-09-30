package com.intema.demo.service;

import com.intema.demo.model.Event;
import com.intema.demo.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.intema.demo.model.User;
import com.intema.demo.model.UserRole;
import com.intema.demo.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {
    private final EventRepository repository;
    private final CurrentUser currentUser;

    public EventService(EventRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public List<Event> getAllEvents() {
        return repository.findAll(Sort.by("data").ascending());
    }

    public Event getEvent(Long id) {
        return repository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional
    public Event saveEvent(Event event) {
        User actor = currentUser.require();
        event.setId(null);
        event.setOwnerId(actor.getId());
        return repository.save(event);
    }

    @Transactional
    public Event updateEvent(Long id, Event event) {
        Event stored = getEvent(id);
        requireWritePermission(stored);
        // Copy only editable fields; a client can never transfer ownership.
        stored.setTitolo(event.getTitolo());
        stored.setDescrizione(event.getDescrizione());
        stored.setData(event.getData());
        stored.setLuogo(event.getLuogo());
        stored.setCategoria(event.getCategoria());
        stored.setLatitude(event.getLatitude());
        stored.setLongitude(event.getLongitude());
        return repository.save(stored);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = getEvent(id);
        requireWritePermission(event);
        repository.delete(event);
    }

    private void requireWritePermission(Event event) {
        User actor = currentUser.require();
        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(event.getOwnerId())) {
            throw new AccessDeniedException("Solo il proprietario o un amministratore può modificare questo evento");
        }
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
        return repository.findAll((root, query, criteria) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (categoria != null && !categoria.isBlank()) {
                predicates.add(criteria.equal(criteria.lower(root.get("categoria")),
                        categoria.trim().toLowerCase(java.util.Locale.ROOT)));
            }
            if (start != null) predicates.add(criteria.greaterThanOrEqualTo(root.get("data"), start));
            if (end != null) predicates.add(criteria.lessThanOrEqualTo(root.get("data"), end));
            return criteria.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable);
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("La data iniziale deve precedere quella finale");
        }
    }
}
