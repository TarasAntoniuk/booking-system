package com.tarasantoniuk.booking.config;

import java.math.BigDecimal;

/**
 * Pricing constants for booking calculations.
 * Centralizes all pricing-related business rules to ensure consistency.
 */
public final class PricingConstants {

    private PricingConstants() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * Markup rate applied to base cost (15%).
     * This rate is added to the base price to calculate additional charges.
     * Example: €100 base cost × 0.15 = €15 markup
     */
    public static final BigDecimal MARKUP_RATE = BigDecimal.valueOf(0.15);

    /**
     * Markup multiplier for total cost calculation (1.15).
     * Represents: base cost + markup = base cost × 1.15
     * Calculated as: 1.0 + MARKUP_RATE
     * Example: €100 base cost × 1.15 = €115 total
     */
    public static final BigDecimal MARKUP_MULTIPLIER =
            BigDecimal.ONE.add(MARKUP_RATE);
}
