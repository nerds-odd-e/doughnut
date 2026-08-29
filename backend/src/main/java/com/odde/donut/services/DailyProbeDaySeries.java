package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.DailyProbeDay;
import com.odde.donut.entities.DailyProbe;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DailyProbeDaySeries {
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private DailyProbeDaySeries() {}

  static List<DailyProbeDay> from(Iterable<DailyProbe> probes, ZoneId zoneId) {
    Map<LocalDate, DailyProbe> latestByLocalDay = new HashMap<>();
    for (DailyProbe probe : probes) {
      LocalDate localDay = probe.getCompletedAt().toInstant().atZone(zoneId).toLocalDate();
      DailyProbe existing = latestByLocalDay.get(localDay);
      if (existing == null || probe.getCompletedAt().after(existing.getCompletedAt())) {
        latestByLocalDay.put(localDay, probe);
      }
    }
    return latestByLocalDay.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> day(entry.getKey(), entry.getValue()))
        .toList();
  }

  private static DailyProbeDay day(LocalDate localDay, DailyProbe probe) {
    return new DailyProbeDay(
        localDay.format(ISO_DATE), probe.getSpeed(), probe.getLapseCount(), probe.getVariability());
  }
}
