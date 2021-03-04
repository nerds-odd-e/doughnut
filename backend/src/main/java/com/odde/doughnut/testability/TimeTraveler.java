package com.odde.doughnut.testability;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.sql.Timestamp;
import java.time.LocalDate;

@Component
@SessionScope
public class TimeTraveler {
    private Timestamp timestamp = null;

    public void timeTravelTo(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp getCurrentUTCTimestamp() {
        if (timestamp == null) {
            return new Timestamp(System.currentTimeMillis());
        }
        return timestamp;
    }
}
