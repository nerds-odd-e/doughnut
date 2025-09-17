package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "games")
public class Games extends EntityIdentifiedByIdOnly {

  @Column(name = "num_of_players")
  private Integer numberOfPlayers;

  @Column(name = "winner")
  private String winner;

  @Column(name = "max_steps")
  private int maxSteps;

  @Column(name = "created_at")
  private Timestamp createdAt;

  @Column(name = "end_date")
  private Timestamp endDate;

  @Column(name = "update_date")
  private Timestamp updatedDate;
}
