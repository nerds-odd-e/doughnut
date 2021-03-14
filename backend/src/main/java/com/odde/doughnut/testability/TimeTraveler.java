package com.odde.doughnut.testability;

import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.ReadRandomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.sql.Timestamp;

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

    public Randomizer getRandomizer() {
        if (timestamp == null) {
            return new ReadRandomizer();
        }
        return new NonRandomizer();
    }
}
