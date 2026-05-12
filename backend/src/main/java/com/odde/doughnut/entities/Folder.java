package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "folder")
public class Folder extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  @NotNull
  private Notebook notebook;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_folder_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Folder parentFolder;

  /** Cached designated folder index note (title {@code index} by convention). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "index_note_id")
  @JsonIgnore
  @Getter
  @Setter
  private Note indexNote;

  /** Container-owned index markdown (populated by migration; canonical from 10.15 onward). */
  @Column(name = "index_content", columnDefinition = "mediumtext")
  @Getter
  @Setter
  private String indexContent;

  @Column(name = "name")
  @NotNull
  @Size(min = 1, max = 512)
  @Getter
  @Setter
  private String name;

  @Column(name = "created_at")
  @NotNull
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at")
  @NotNull
  @Getter
  @Setter
  private Timestamp updatedAt;

  /** Serialized as {@code parentFolderId} for API consumers (parent association stays lazy). */
  @Schema(description = "Parent folder id when nested; omitted at notebook root.")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Integer getParentFolderId() {
    return parentFolder == null ? null : parentFolder.getId();
  }
}
