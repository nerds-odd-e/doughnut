package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_level_index")
public class NoteLevelIndex {

  @Id
  @Column(name = "note_id")
  @Getter
  private Integer id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "note_id")
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @Column(name = "level", nullable = false)
  @NotNull
  @Getter
  @Setter
  private Integer level;
}
