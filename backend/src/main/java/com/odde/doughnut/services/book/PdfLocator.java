package com.odde.doughnut.services.book;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonPropertyOrder({"type", "pageIndex", "bbox"})
public record PdfLocator(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pageIndex,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Double> bbox)
    implements ContentLocator {}
