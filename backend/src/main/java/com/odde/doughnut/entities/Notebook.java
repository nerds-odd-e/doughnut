package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notebook")
public class Notebook extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creatorEntity;

  @OneToOne
  @JoinColumn(name = "ownership_id")
  @Getter
  @Setter
  private Ownership ownership;

  @JoinTable(
      name = "notebook_head_note",
      joinColumns = {@JoinColumn(name = "notebook_id", referencedColumnName = "id")},
      inverseJoinColumns = {@JoinColumn(name = "head_note_id", referencedColumnName = "id")})
  @OneToOne
  @Getter
  @Setter
  private Note headNote;

  @OneToMany(mappedBy = "notebook")
  @JsonIgnore
  @Getter
  @Setter
  private List<Note> notes = new ArrayList<>();

  @Column(name = "skip_review_entirely")
  @Getter
  @Setter
  Boolean skipReviewEntirely = false;

  @Column(name = "deleted_at")
  @Setter
  private Timestamp deletedAt;
}
