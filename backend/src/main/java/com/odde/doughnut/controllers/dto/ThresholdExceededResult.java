package com.odde.doughnut.controllers.dto;

public record ThresholdExceededResult(
    boolean thresholdExceeded, int wrongCount, int threshold, int periodDays) {}
