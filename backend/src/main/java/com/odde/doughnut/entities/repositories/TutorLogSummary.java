package com.odde.doughnut.entities.repositories;

import java.sql.Timestamp;

public record TutorLogSummary(long logCount, Timestamp lastRecordedAt) {}
