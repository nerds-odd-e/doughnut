package com.odde.doughnut.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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

  @Column(name = "skip_review")
  @Getter
  @Setter
  private Boolean skipReview = false;

  @Getter @Setter private Integer level = defaultLevel;
}
