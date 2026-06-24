package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_alias_index")
public class NoteAliasIndex extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notebook_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Notebook notebook;

  @Column(name = "alias_display", nullable = false, length = 767)
  @NotNull
  @Size(max = 767)
  @Getter
  @Setter
  private String aliasDisplay;

  @Column(name = "alias_lookup_key", nullable = false, length = 767)
  @NotNull
  @Size(max = 767)
  @Getter
  @Setter
  private String aliasLookupKey;
}
