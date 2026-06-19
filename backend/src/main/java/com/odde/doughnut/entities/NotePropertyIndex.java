package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_property_index")
public class NotePropertyIndex extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @Column(name = "property_key", nullable = false, length = 255)
  @NotNull
  @Size(max = 255)
  @Getter
  @Setter
  private String propertyKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note targetNote;
}
