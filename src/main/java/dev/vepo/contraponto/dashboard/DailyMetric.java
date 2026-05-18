package dev.vepo.contraponto.dashboard;

import java.time.LocalDate;

public record DailyMetric(LocalDate day, long count) {

    public int barHeightPercent(long max) {
        if (count <= 0) {
            return 0;
        }
        if (max <= 0) {
            return 100;
        }
        return (int) Math.max(4, Math.round(count * 100.0 / max));
    }
}
