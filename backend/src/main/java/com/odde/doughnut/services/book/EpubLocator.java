package com.odde.doughnut.services.book;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"type", "href", "fragment"})
public record EpubLocator(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String href,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String fragment)
    implements ContentLocator {}
