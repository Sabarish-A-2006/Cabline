package com.sclaria.cabbooking.api.dto;

public record AdminMetricsView(long users, long drivers, long rides, long activeRides,
                               long paymentsPending, long paymentsPaid, long ticketsOpen) {}
