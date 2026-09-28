package com.intema.demo.repository;

import com.intema.demo.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCategoriaIgnoreCaseOrderByDataAsc(String categoria);
    List<Event> findByDataBetweenOrderByDataAsc(LocalDateTime start, LocalDateTime end);
    List<Event> findByDataGreaterThanEqualOrderByDataAsc(LocalDateTime now);

    @Query("""
            SELECT e FROM Event e WHERE
            (:categoria IS NULL OR LOWER(e.categoria) = LOWER(:categoria))
            AND (:start IS NULL OR e.data >= :start)
            AND (:end IS NULL OR e.data <= :end)
            """)
    Page<Event> search(@Param("categoria") String categoria,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end,
                       Pageable pageable);
}
