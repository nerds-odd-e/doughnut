package com.odde.doughnut.services.book;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PageBbox(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pageIndex,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Double> bbox,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Integer contentBlockId,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String derivedTitle) {}
