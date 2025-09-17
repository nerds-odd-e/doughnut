package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "players")
public class Players extends EntityIdentifiedByIdOnly {

  @Column(name = "name")
  @NotNull
  private String name;

  @Column(name = "is_admin")
  private Boolean isAdmin;

  @Column(name = "create_date")
  @CreatedDate
  private LocalDateTime createDate;

    @OneToMany(mappedBy = "player")
    private List<Round> players = new ArrayList<>();
}
