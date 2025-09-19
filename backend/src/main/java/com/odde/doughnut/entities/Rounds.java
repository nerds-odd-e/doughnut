package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "rounds")
public class Rounds extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "player_id")
  @JsonIgnore
  private Players player;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id")
  @JsonIgnore
  private Games game;

  @Column(name = "round_no")
  private Integer roundNo;

  @Column(name = "mode")
  private String mode;

  @Column(name = "dice")
  private Integer dice;

  @Column(name = "damage")
  private Integer damage;

  @Column(name = "step")
  private Integer step;

  @Column(name = "create_date")
  //  @CreatedDate
  private Date createDate;

  @Column(name = "update_date")
  private Date updateDate;
}
