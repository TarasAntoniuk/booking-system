package com.tarasantoniuk.event.service;

import com.tarasantoniuk.event.entity.Event;
import com.tarasantoniuk.event.enums.EntityType;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setEventType(EventType.BOOKING_CREATED);
        testEvent.setEntityType(EntityType.BOOKING);
        testEvent.setEntityId(100L);
        testEvent.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create event without event data")
    void shouldCreateEventWithoutEventData() {
        // Given
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.BOOKING_CREATED, 100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEventType()).isEqualTo(EventType.BOOKING_CREATED);
        assertThat(result.getEntityType()).isEqualTo(EntityType.BOOKING);
        assertThat(result.getEntityId()).isEqualTo(100L);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event with event data")
    void shouldCreateEventWithEventData() {
        // Given
        testEvent.setEventData("Additional context");
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.BOOKING_CREATED, 100L, "Additional context");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEventType()).isEqualTo(EventType.BOOKING_CREATED);
        assertThat(result.getEntityId()).isEqualTo(100L);
        assertThat(result.getEventData()).isEqualTo("Additional context");

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event for UNIT_CREATED")
    void shouldCreateEventForUnitCreated() {
        // Given
        testEvent.setEventType(EventType.UNIT_CREATED);
        testEvent.setEntityType(EntityType.UNIT);
        testEvent.setEntityId(5L);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.UNIT_CREATED, 5L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(EventType.UNIT_CREATED);
        assertThat(result.getEntityType()).isEqualTo(EntityType.UNIT);
        assertThat(result.getEntityId()).isEqualTo(5L);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event for BOOKING_CONFIRMED")
    void shouldCreateEventForBookingConfirmed() {
        // Given
        testEvent.setEventType(EventType.BOOKING_CONFIRMED);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.BOOKING_CONFIRMED, 100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(EventType.BOOKING_CONFIRMED);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event for BOOKING_CANCELLED")
    void shouldCreateEventForBookingCancelled() {
        // Given
        testEvent.setEventType(EventType.BOOKING_CANCELLED);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.BOOKING_CANCELLED, 100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(EventType.BOOKING_CANCELLED);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event for PAYMENT_COMPLETED")
    void shouldCreateEventForPaymentCompleted() {
        // Given
        testEvent.setEventType(EventType.PAYMENT_COMPLETED);
        testEvent.setEntityType(EntityType.PAYMENT);
        testEvent.setEntityId(200L);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.PAYMENT_COMPLETED, 200L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(EventType.PAYMENT_COMPLETED);
        assertThat(result.getEntityType()).isEqualTo(EntityType.PAYMENT);
        assertThat(result.getEntityId()).isEqualTo(200L);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event with null event data")
    void shouldCreateEventWithNullEventData() {
        // Given
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.BOOKING_CREATED, 100L, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventData()).isNull();

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create events in batch")
    void shouldCreateEventsInBatch() {
        // Given
        List<Long> entityIds = List.of(1L, 2L, 3L);
        Event event1 = createTestEvent(1L, EventType.BOOKING_EXPIRED, 1L);
        Event event2 = createTestEvent(2L, EventType.BOOKING_EXPIRED, 2L);
        Event event3 = createTestEvent(3L, EventType.BOOKING_EXPIRED, 3L);
        List<Event> savedEvents = List.of(event1, event2, event3);

        when(eventRepository.saveAll(anyList())).thenReturn(savedEvents);

        // When
        List<Event> result = eventService.createEventsInBatch(EventType.BOOKING_EXPIRED, entityIds);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrderElementsOf(savedEvents);

        verify(eventRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should create events in batch with correct event type")
    void shouldCreateEventsInBatchWithCorrectEventType() {
        // Given
        List<Long> entityIds = List.of(10L, 20L);
        when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<Event> result = eventService.createEventsInBatch(EventType.BOOKING_CANCELLED, entityIds);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getEventType() == EventType.BOOKING_CANCELLED);
        assertThat(result).allMatch(e -> e.getEntityType() == EntityType.BOOKING);
        assertThat(result).extracting(Event::getEntityId).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    @DisplayName("Should handle empty list in batch creation")
    void shouldHandleEmptyListInBatchCreation() {
        // Given
        List<Long> emptyIds = List.of();
        when(eventRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        List<Event> result = eventService.createEventsInBatch(EventType.BOOKING_EXPIRED, emptyIds);

        // Then
        assertThat(result).isEmpty();
        verify(eventRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should create single event in batch")
    void shouldCreateSingleEventInBatch() {
        // Given
        List<Long> singleId = List.of(42L);
        Event savedEvent = createTestEvent(1L, EventType.PAYMENT_COMPLETED, 42L);
        when(eventRepository.saveAll(anyList())).thenReturn(List.of(savedEvent));

        // When
        List<Event> result = eventService.createEventsInBatch(EventType.PAYMENT_COMPLETED, singleId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntityId()).isEqualTo(42L);
        assertThat(result.get(0).getEventType()).isEqualTo(EventType.PAYMENT_COMPLETED);
    }

    private Event createTestEvent(Long id, EventType eventType, Long entityId) {
        Event event = new Event();
        event.setId(id);
        event.setEventType(eventType);
        event.setEntityId(entityId);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
