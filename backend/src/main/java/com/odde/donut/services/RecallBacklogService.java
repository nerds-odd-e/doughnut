package com.odde.donut.services;

import static java.time.format.DateTimeFormatter.ISO_DATE;

import com.odde.donut.controllers.dto.RecallBacklogForecast;
import com.odde.donut.controllers.dto.RecallStatsDTO.DayCount;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecallBacklogService {
  private static final int WINDOW_DAYS = 30;

  private final MemoryTrackerRepository memoryTrackerRepository;
  private final EntityPersister entityPersister;

  public RecallBacklogService(
      MemoryTrackerRepository memoryTrackerRepository, EntityPersister entityPersister) {
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityPersister = entityPersister;
  }

  public RecallBacklogForecast spreadOutBacklog(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    List<Timestamp> dayStarts = new ArrayList<>();
    dayStarts.add(TimestampOperations.getStartOfDay(currentUTCTimestamp, timeZone));
    for (int day = 0; day <= WINDOW_DAYS; day++) {
      dayStarts.add(TimestampOperations.getStartOfNextDay(dayStarts.get(day), timeZone));
    }

    List<MemoryTracker> backlog = new ArrayList<>();
    int[] counts = new int[WINDOW_DAYS + 1];
    memoryTrackerRepository
        .findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
            user.getId(), dayStarts.get(WINDOW_DAYS + 1))
        .forEach(tracker -> {
          int day = dayOf(tracker.getNextRecallAt(), dayStarts);
          if (day == 0) backlog.add(tracker);
          else counts[day]++;
        });

    int[] additions = additionsUpToLevel(backlog.size(), counts);
    int next = additions[0];
    for (int day = 1; day <= WINDOW_DAYS; day++) {
      Timestamp placement = TimestampOperations.addHoursToTimestamp(dayStarts.get(day), 1);
      for (int i = 0; i < additions[day]; i++) {
        MemoryTracker tracker = backlog.get(next++);
        tracker.setNextRecallAt(placement);
        entityPersister.save(tracker);
      }
      counts[day] += additions[day];
    }

    List<DayCount> days = new ArrayList<>();
    int sum = 0;
    for (int day = 1; day <= WINDOW_DAYS; day++) {
      days.add(
          new DayCount(
              dayStarts.get(day).toInstant().atZone(timeZone).toLocalDate().format(ISO_DATE),
              counts[day]));
      sum += counts[day];
    }
    return new RecallBacklogForecast(additions[0], (double) sum / WINDOW_DAYS, days);
  }

  private static int dayOf(Timestamp nextRecallAt, List<Timestamp> dayStarts) {
    int day = 0;
    while (nextRecallAt.after(dayStarts.get(day + 1))) day++;
    return day;
  }

  /**
   * Water-fills today and the window days to the smallest whole level that absorbs the backlog;
   * when the backlog does not divide evenly, earlier days take the extra. Index 0 is how many
   * backlog trackers today keeps; other indexes are how many each day receives.
   */
  private static int[] additionsUpToLevel(int backlogSize, int[] counts) {
    int level = 0;
    while (capacity(level, counts) < backlogSize) level++;
    int[] additions = new int[counts.length];
    int remaining = backlogSize;
    for (int day = 0; day < counts.length; day++) {
      additions[day] = Math.max(0, level - 1 - counts[day]);
      remaining -= additions[day];
    }
    for (int day = 0; day < counts.length && remaining > 0; day++) {
      if (counts[day] + additions[day] < level) {
        additions[day]++;
        remaining--;
      }
    }
    return additions;
  }

  private static int capacity(int level, int[] counts) {
    int capacity = 0;
    for (int count : counts) capacity += Math.max(0, level - count);
    return capacity;
  }
}
