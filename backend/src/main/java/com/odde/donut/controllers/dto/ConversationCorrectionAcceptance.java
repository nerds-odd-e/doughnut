package com.odde.donut.controllers.dto;

import jakarta.validation.constraints.NotNull;

public record ConversationCorrectionAcceptance(
    @NotNull String reviewedValue, String baseRevision) {}
