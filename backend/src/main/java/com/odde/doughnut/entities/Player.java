package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "player")
public class Player extends EntityIdentifiedByIdOnly {

  @Column(name = "name")
  @Getter
  @Setter
  @NotNull
  private String name;

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

  @Column(name = "id_of_game")
  @Getter
  @Setter
  @NotNull
  private Integer idOfGame;

  public Player() {
    this.createDate = new Timestamp(System.currentTimeMillis());
    this.updateDate = new Timestamp(System.currentTimeMillis());
  }

  public Player(String name, Integer idOfGame) {
    this();
    this.name = name;
    this.idOfGame = idOfGame;
  }
}
