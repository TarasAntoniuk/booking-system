package com.tarasantoniuk.event.service;

import com.tarasantoniuk.event.entity.Event;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        testEvent.setEntityId(5L);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.UNIT_CREATED, 5L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(EventType.UNIT_CREATED);
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
        testEvent.setEntityId(200L);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(EventType.PAYMENT_COMPLETED, 200L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(EventType.PAYMENT_COMPLETED);
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
}
