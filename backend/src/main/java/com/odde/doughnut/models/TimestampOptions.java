package com.odde.doughnut.models;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public abstract class TimestampOptions {
    public static Timestamp addDaysToTimestamp(Timestamp timestamp, int daysToAdd) {
        ZonedDateTime zonedDateTime = timestamp.toInstant().atZone(ZoneId.of("UTC"));
        return Timestamp.from(zonedDateTime.plus(daysToAdd, ChronoUnit.DAYS).toInstant());
    }
}
