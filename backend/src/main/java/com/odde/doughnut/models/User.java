package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "user")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @Getter @Setter private String name;
  @Column(name="external_identifier")
  @Getter @Setter private String externalIdentifier;

  @OneToMany(mappedBy="user")
  @OrderBy("created_datetime DESC")
  @Getter private List<Note> notes;
}
