package com.odde.doughnut.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "race_game_progress")
@Getter
@Setter
public class RaceGameProgress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "player_id", nullable = false, unique = true)
  private String playerId;

  @Column(name = "car_position", nullable = false)
  private int carPosition = 0;

  @Column(name = "round_count", nullable = false)
  private int roundCount = 0;

  @Column(name = "last_dice_face")
  private Integer lastDiceFace;
}
