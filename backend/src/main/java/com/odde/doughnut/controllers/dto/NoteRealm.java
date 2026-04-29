package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@JsonPropertyOrder({
  "id",
  "slug",
  "note",
  "fromBazaar",
  "circle",
  "children",
  "inboundReferences",
  "notebook"
})
public class NoteRealm {
  @Getter @Setter private List<Note> inboundReferences;

  @NotNull @Getter private Note note;

  @Getter @Setter private Boolean fromBazaar;

  public NoteRealm(Note note) {
    this.note = note;
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }

  @NonNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public String getSlug() {
    return Objects.requireNonNullElse(note.getSlug(), "");
  }

  public List<Note> getChildren() {
    return note.getChildren();
  }

  @NonNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public Notebook getNotebook() {
    return Objects.requireNonNull(note.getNotebook());
  }
}
