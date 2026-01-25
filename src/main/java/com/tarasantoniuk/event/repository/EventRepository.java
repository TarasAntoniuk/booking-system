package com.tarasantoniuk.event.repository;

import com.tarasantoniuk.event.entity.Event;
import com.tarasantoniuk.event.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByEventType(EventType eventType);

    List<Event> findByEntityId(Long entityId);

    List<Event> findByEventTypeAndEntityId(EventType eventType, Long entityId);
}