package com.intema.demo.repository;

import com.intema.demo.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    List<Event> findByCategoriaIgnoreCaseOrderByDataAsc(String categoria);
    List<Event> findByDataBetweenOrderByDataAsc(LocalDateTime start, LocalDateTime end);
    List<Event> findByDataGreaterThanEqualOrderByDataAsc(LocalDateTime now);

}
