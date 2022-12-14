package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review_setting")
public class ReviewSetting {

  public static final Integer defaultLevel = 0;

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "remember_spelling")
  @Getter
  @Setter
  private Boolean rememberSpelling = false;

  @Getter @Setter private Integer level = defaultLevel;
}
