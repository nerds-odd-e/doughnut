package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.donut.entities.converters.DisplayNameConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "folder")
public class Folder extends EntityIdentifiedByIdOnly {

  public static final int MAX_NAME_LENGTH = 512;

  @Formula("id in (select tf.id from trashed_folder tf)")
  private boolean trashedInDatabase;

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

  /** Container-owned readme markdown. */
  @Column(name = "readme_content", columnDefinition = "mediumtext")
  @Getter
  @Setter
  private String readmeContent;

  @Column(name = "name")
  @Convert(converter = DisplayNameConverter.class)
  @JsonIgnore
  private DisplayName name = new DisplayName("");

  @NotNull
  @Size(min = 1, max = MAX_NAME_LENGTH)
  @JsonProperty("name")
  public String getName() {
    return name.value();
  }

  public void setName(DisplayName name) {
    this.name = name;
  }

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

  @JsonIgnore
  public boolean isTrashed() {
    return getParentFolder() == null
        ? getName().equalsIgnoreCase("_trash")
        : getParentFolder().isTrashed();
  }

  @JsonIgnore
  public void requireInNotebook(Notebook notebook) {
    if (!getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
  }
}
