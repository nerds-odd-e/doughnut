package com.odde.doughnut.services.book;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = PdfLocator.class, name = "pdf"),
  @JsonSubTypes.Type(value = EpubLocator.class, name = "epub"),
})
@Schema(
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "pdf", schema = PdfLocator.class),
      @DiscriminatorMapping(value = "epub", schema = EpubLocator.class)
    })
public sealed interface ContentLocator permits PdfLocator, EpubLocator {}
