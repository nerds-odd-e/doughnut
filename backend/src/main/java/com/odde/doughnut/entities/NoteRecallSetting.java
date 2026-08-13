package com.odde.doughnut.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Embeddable
public class NoteRecallSetting {
  public static final Integer defaultLevel = 0;

  @Column(name = "remember_spelling")
  @Getter
  @Setter
  private Boolean rememberSpelling = false;

  @Getter @Setter private Integer level = defaultLevel;
}
