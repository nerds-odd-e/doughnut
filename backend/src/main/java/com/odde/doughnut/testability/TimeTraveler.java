package com.odde.doughnut.testability;

import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.sql.Timestamp;

@Component
@SessionScope
public class TimeTraveler {
    private Timestamp timestamp = null;
    private NonRandomizer nonRandomizer = null;

    public void timeTravelTo(Timestamp timestamp) {
        this.timestamp = timestamp;
        if (nonRandomizer == null) {
            nonRandomizer = new NonRandomizer();
        }
    }

    public Timestamp getCurrentUTCTimestamp() {
        if (timestamp == null) {
            return new Timestamp(System.currentTimeMillis());
        }
        return timestamp;
    }

    public Randomizer getRandomizer() {
        if (nonRandomizer == null) {
            return new RealRandomizer();
        }
        return nonRandomizer;
    }

    public void setAlwaysChoose(String option) {
        if (nonRandomizer == null) {
            nonRandomizer = new NonRandomizer();
        }
        nonRandomizer.setAlwaysChoose(option);
    }
}
