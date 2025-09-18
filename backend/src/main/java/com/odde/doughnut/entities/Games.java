package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
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

  @Column(name = "created_date")
  private Date createdDate;

  @Column(name = "end_date")
  private Date endDate;

  @Column(name = "update_date")
  private Date updatedDate;

  @OneToMany(mappedBy = "game")
  private List<Rounds> rounds = new ArrayList<>();
}
