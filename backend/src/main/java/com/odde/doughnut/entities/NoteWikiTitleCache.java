package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_wiki_title_cache")
public class NoteWikiTitleCache extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_note_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Note targetNote;

  @Column(name = "link_text", nullable = false, length = 767)
  @NotNull
  @Size(max = 767)
  @Getter
  @Setter
  private String linkText;
}
