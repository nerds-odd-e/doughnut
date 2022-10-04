package com.odde.doughnut.entities;

import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.springframework.lang.Nullable;

import lombok.Getter;
import lombok.Setter;

@Embeddable
public class NoteLocation {

  @Column(name = "latitude")
  @Getter
  @Setter
  @Nullable
  public String latitude;


  @Column(name = "longitude")
  @Getter
  @Setter
  @Nullable
  public String longitude;
}
