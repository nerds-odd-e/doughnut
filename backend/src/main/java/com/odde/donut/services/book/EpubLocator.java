package com.odde.donut.services.book;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"type", "href", "fragment", "cfi"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EpubLocator(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String href,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String fragment,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String cfi)
    implements ContentLocator {}
