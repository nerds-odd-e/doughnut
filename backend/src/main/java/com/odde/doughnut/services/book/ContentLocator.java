package com.odde.doughnut.services.book;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = PdfLocator.class, name = "PdfLocator_Full"),
  @JsonSubTypes.Type(value = EpubLocator.class, name = "EpubLocator_Full"),
})
@Schema(
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "PdfLocator_Full", schema = PdfLocator.class),
      @DiscriminatorMapping(value = "EpubLocator_Full", schema = EpubLocator.class)
    })
public sealed interface ContentLocator permits PdfLocator, EpubLocator {}
