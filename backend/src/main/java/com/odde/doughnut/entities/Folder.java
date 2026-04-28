package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @Column(name = "name")
  @NotNull
  @Size(min = 1, max = 512)
  @Getter
  @Setter
  private String name;

  @Column(name = "slug")
  @Size(max = 767)
  @Getter
  @Setter
  @JsonIgnore
  private String slug;

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
}
