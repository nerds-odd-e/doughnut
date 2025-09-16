package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "player")
public class Player extends EntityIdentifiedByIdOnly {

  @Column(name = "name")
  @NotNull
  private String name;

  @Column(name = "is_admin")
  private Boolean isAdmin;

  @Column(name = "create_date")
  @CreatedDate
  private Timestamp createDate;
}
