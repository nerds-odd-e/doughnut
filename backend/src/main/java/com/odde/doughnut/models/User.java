package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "user")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @Getter @Setter private String name;
}
