package com.odde.donut.controllers.dto;

import com.odde.donut.entities.Folder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "One folder on a folder trail: only its id and name.")
public record FolderTrailSegment(@NotNull Integer id, @NotNull String name) {

  public static List<FolderTrailSegment> of(List<Folder> folders) {
    return folders.stream().map(f -> new FolderTrailSegment(f.getId(), f.getName())).toList();
  }
}
