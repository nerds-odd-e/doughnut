package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "image")
public class Image extends Attachment {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note note;
}
