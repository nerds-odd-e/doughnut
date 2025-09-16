package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "round")
public class Round extends EntityIdentifiedByIdOnly {

  @Column(name = "player_id")
  private Integer player_id;

  @Column(name = "game_id")
  private Integer game_id;

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
  private Timestamp createDate;

  @Column(name = "update_date")
  private Timestamp updateDate;
}
