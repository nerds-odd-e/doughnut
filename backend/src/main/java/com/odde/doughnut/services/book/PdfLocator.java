package com.odde.doughnut.services.book;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonPropertyOrder({"type", "pageIndex", "bbox", "contentBlockId", "derivedTitle"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PdfLocator(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pageIndex,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Double> bbox,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Integer contentBlockId,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String derivedTitle)
    implements ContentLocator {}
