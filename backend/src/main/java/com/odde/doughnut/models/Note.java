package com.odde.doughnut.models;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "note")
public class Note {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @Getter
  @Setter private String title;
  @Getter @Setter private String description;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  private User user;

  @Column(name="created_datetime")
  @Getter @Setter private Date createdDatetime;
}
