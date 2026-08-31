package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resolved_wiki_link")
public class ResolvedWikiLink extends EntityIdentifiedByIdOnly {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_note_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Note sourceNote;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destination_note_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Note destinationNote;

  @Column(name = "authored_link", nullable = false, length = 767)
  @NotNull
  @Size(max = 767)
  @Getter
  @Setter
  private String authoredLink;
}
