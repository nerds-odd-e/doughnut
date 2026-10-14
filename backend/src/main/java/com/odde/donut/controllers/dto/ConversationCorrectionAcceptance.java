package com.odde.donut.controllers.dto;

import jakarta.validation.constraints.NotNull;

public record ConversationCorrectionAcceptance(
    @NotNull String reviewedValue,
    String baseRevision,
    TitleRenameReferenceHandling referenceHandling) {
  public ConversationCorrectionAcceptance(String reviewedValue, String baseRevision) {
    this(reviewedValue, baseRevision, null);
  }
}
