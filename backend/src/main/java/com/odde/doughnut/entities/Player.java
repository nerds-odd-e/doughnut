package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = "player")
public class Player extends EntityIdentifiedByIdOnly {
  @Column(name = "name")
  private String name;

  @Column(name = "cur_steps")
  private String curSteps;

  @Column(name = "damage")
  private Integer damage;
}
