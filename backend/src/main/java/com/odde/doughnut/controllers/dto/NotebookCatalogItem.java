package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = NotebookCatalogNotebookItem.class, name = "notebook"),
  @JsonSubTypes.Type(value = NotebookCatalogGroupItem.class, name = "notebookGroup"),
})
@Schema(
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "notebook", schema = NotebookCatalogNotebookItem.class),
      @DiscriminatorMapping(value = "notebookGroup", schema = NotebookCatalogGroupItem.class)
    })
public sealed interface NotebookCatalogItem
    permits NotebookCatalogNotebookItem, NotebookCatalogGroupItem {}
