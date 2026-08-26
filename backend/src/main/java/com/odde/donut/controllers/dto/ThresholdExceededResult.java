package com.odde.donut.controllers.dto;

public record ThresholdExceededResult(
    boolean thresholdExceeded, int wrongCount, int threshold, int periodDays) {}
