package dev.vepo.contraponto.platforminsights;

import java.time.LocalDate;

public record DailyVisitorSplit(LocalDate day, long registeredVisitors, long guestVisitors) {

    public int registeredBarHeightPercent(long max) {
        return barHeightPercent(registeredVisitors, max);
    }

    public int guestBarHeightPercent(long max) {
        return barHeightPercent(guestVisitors, max);
    }

    private int barHeightPercent(long count, long max) {
        if (count <= 0) {
            return 0;
        }
        if (max <= 0) {
            return 100;
        }
        return (int) Math.max(4, Math.round(count * 100.0 / max));
    }
}
