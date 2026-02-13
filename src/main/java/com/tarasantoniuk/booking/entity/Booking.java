package com.tarasantoniuk.booking.entity;

import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bookings_id_seq")
    @SequenceGenerator(name = "bookings_id_seq", sequenceName = "bookings_id_seq", allocationSize = 50)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Cancel this booking. Both PENDING and CONFIRMED bookings can be cancelled.
     * Note: Refund logic for CONFIRMED bookings with completed payments is not yet implemented.
     *
     * @return the previous status before cancellation (useful for determining if refund is needed)
     */
    public BookingStatus cancel() {
        if (this.status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        BookingStatus previousStatus = this.status;
        this.status = BookingStatus.CANCELLED;
        return previousStatus;
    }

    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed");
        }
        this.status = BookingStatus.CONFIRMED;
        this.expiresAt = null;
    }

    public boolean isExpired() {
        return this.expiresAt != null && !LocalDateTime.now().isBefore(this.expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return id != null && id.equals(booking.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}