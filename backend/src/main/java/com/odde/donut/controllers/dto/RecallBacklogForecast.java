package com.odde.donut.controllers.dto;

import com.odde.donut.controllers.dto.RecallStatsDTO.DayCount;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecallBacklogForecast(
    @NotNull Integer todayCount, @NotNull Double averageCount, @NotNull List<DayCount> days) {}
