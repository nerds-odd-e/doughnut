package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Notebook;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

@JsonPropertyOrder({"type", "id", "name", "createdAt", "notebooks"})
public final class NotebookCatalogGroupItem implements NotebookCatalogItem {
  @NotNull public Integer id;
  @NotNull public String name;
  @NotNull public Timestamp createdAt;
  @NotNull public List<Notebook> notebooks;

  public NotebookCatalogGroupItem() {}

  public NotebookCatalogGroupItem(
      Integer id, String name, Timestamp createdAt, List<Notebook> notebooks) {
    this.id = id;
    this.name = name;
    this.createdAt = createdAt;
    this.notebooks = notebooks;
  }
}
