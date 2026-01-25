package com.tarasantoniuk.event.service;

import com.tarasantoniuk.event.entity.Event;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        event.setEntityId(entityId);
        event.setEventData(eventData);

        Event saved = eventRepository.save(event);
        log.info("Event created: {} for entity: {}", eventType, entityId);

        return saved;
    }
}