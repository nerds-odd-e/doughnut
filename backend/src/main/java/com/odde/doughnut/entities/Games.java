package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
  private LocalDateTime createdAt;

  @Column(name = "end_date")
  private LocalDateTime endDate;

  @Column(name = "update_date")
  private LocalDateTime updatedDate;

  @OneToMany(mappedBy = "game")
  private List<Rounds> rounds = new ArrayList<>();
}
