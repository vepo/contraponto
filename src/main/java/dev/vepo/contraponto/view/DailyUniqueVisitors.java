package dev.vepo.contraponto.view;

public record DailyUniqueVisitors(long registeredVisitors, long guestVisitors) {

    public long totalVisitors() {
        return registeredVisitors + guestVisitors;
    }
}
