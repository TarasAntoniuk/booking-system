package com.tarasantoniuk.unit.entity;

import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.tarasantoniuk.booking.config.PricingConstants.MARKUP_MULTIPLIER;

@Entity
@Table(name = "units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "units_id_seq")
    @SequenceGenerator(name = "units_id_seq", sequenceName = "units_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "number_of_rooms", nullable = false)
    private Integer numberOfRooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "accommodation_type", nullable = false, length = 50)
    private AccommodationType accommodationType;

    @Column(nullable = false)
    private Integer floor;

    @Column(name = "base_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseCost;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate total cost with 15% markup
     */
    public BigDecimal getTotalCost() {
        return baseCost.multiply(MARKUP_MULTIPLIER);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unit unit)) return false;
        return id != null && id.equals(unit.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}