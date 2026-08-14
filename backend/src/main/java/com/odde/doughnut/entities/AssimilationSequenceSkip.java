package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assimilation_sequence_skip")
@Getter
@Setter
public class AssimilationSequenceSkip extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "note_id", nullable = false)
  @JsonIgnore
  private Note note;

  @JsonProperty
  public Integer getNoteId() {
    return note.getId();
  }

  @Column(name = "property_key", nullable = false)
  private String propertyKey = "";

  @Column(name = "skipped_at", nullable = false)
  private Timestamp skippedAt;

  /** JPQL fragment for joined alias {@code n}: note-level sequence skip for {@code :userId}. */
  public static final String JPA_NOT_EXISTS_NOTE_LEVEL_SKIP =
      "NOT EXISTS (SELECT skip FROM AssimilationSequenceSkip skip"
          + " WHERE skip.note = n AND skip.user.id = :userId AND skip.propertyKey = '')";

  /**
   * JPQL fragment for joined aliases {@code n} and {@code i}: property-grain sequence skip for
   * {@code :userId}.
   */
  public static final String JPA_NOT_EXISTS_PROPERTY_SKIP =
      "NOT EXISTS (SELECT skip FROM AssimilationSequenceSkip skip"
          + " WHERE skip.note = n AND skip.user.id = :userId AND skip.propertyKey = i.propertyKey)";
}
