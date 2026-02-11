package com.tarasantoniuk.event.service;

import com.tarasantoniuk.event.entity.Event;
import com.tarasantoniuk.event.enums.EntityType;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    @Transactional
    public Event createEvent(EventType eventType, Long entityId) {
        return createEvent(eventType, entityId, null);
    }

    @Transactional
    public Event createEvent(EventType eventType, Long entityId, String eventData) {
        Event event = new Event();
        event.setEventType(eventType);
        event.setEntityType(resolveEntityType(eventType));
        event.setEntityId(entityId);
        event.setEventData(eventData);

        Event saved = eventRepository.save(event);
        log.info("Event created: {} for entity: {}", eventType, entityId);

        return saved;
    }

    @Transactional
    public List<Event> createEventsInBatch(EventType eventType, List<Long> entityIds) {
        List<Event> events = entityIds.stream()
                .map(entityId -> {
                    Event event = new Event();
                    event.setEventType(eventType);
                    event.setEntityType(resolveEntityType(eventType));
                    event.setEntityId(entityId);
                    return event;
                })
                .toList();

        List<Event> saved = eventRepository.saveAll(events);
        log.info("Batch created {} events of type {}", saved.size(), eventType);

        return saved;
    }

    private EntityType resolveEntityType(EventType eventType) {
        return switch (eventType) {
            case UNIT_CREATED -> EntityType.UNIT;
            case BOOKING_CREATED, BOOKING_CONFIRMED, BOOKING_CANCELLED, BOOKING_EXPIRED -> EntityType.BOOKING;
            case PAYMENT_COMPLETED -> EntityType.PAYMENT;
        };
    }
}