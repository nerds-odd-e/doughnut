package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "step")
public class Step extends EntityIdentifiedByIdOnly {

  @Column(name = "move")
  @Getter
  @Setter
  private String move;

  @Column(name = "damage")
  @Getter
  @Setter
  private Integer damage;

  @Column(name = "current_step")
  @Getter
  @Setter
  @NotNull
  private Integer currentStep;

  @Column(name = "create_date")
  @Getter
  @Setter
  @NotNull
  private Timestamp createDate;

  @Column(name = "update_date")
  @Getter
  @Setter
  @NotNull
  private Timestamp updateDate;

  @Column(name = "player_id")
  @Getter
  @Setter
  @NotNull
  private Integer playerId;

  public Step() {
    this.createDate = new Timestamp(System.currentTimeMillis());
    this.updateDate = new Timestamp(System.currentTimeMillis());
  }

  public Step(String move, Integer damage, Integer currentStep, Integer playerId) {
    this();
    this.move = move;
    this.damage = damage;
    this.currentStep = currentStep;
    this.playerId = playerId;
  }
}
