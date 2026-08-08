package com.odde.doughnut.entities.repositories;

import java.sql.Timestamp;

public record RecordedFeedbackSummary(long sessionCount, Timestamp lastRecordedAt) {}
