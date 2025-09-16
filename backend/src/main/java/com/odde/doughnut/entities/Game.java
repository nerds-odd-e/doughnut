package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Entity
@Table(name = "game")
public class Game extends EntityIdentifiedByIdOnly {

  @Column(name = "name")
  @Getter
  @Setter
  private String name;

  @Column(name = "no_players")
  @Getter
  @Setter
  private Integer noPlayers;

  @Column(name = "winning_step")
  @Getter
  @Setter
  private Integer winningStep;

  @Column(name = "name_of_winner")
  @Getter
  @Setter
  private String nameOfWinner;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at")
  @Getter
  @Setter
  private Timestamp updatedAt;

    public Game(Integer id, String name, Integer noPlayers, String nameOfWinner, Integer winningStep) {
        this.id = id;
        this.name = name;
        this.noPlayers = noPlayers;
        this.nameOfWinner = nameOfWinner;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = this.createdAt;
        this.winningStep = winningStep;
    }

    public Game() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = this.createdAt;
    }
}
