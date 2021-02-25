package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "link")
public class BazaarNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne
    @JoinColumn(name = "note", referencedColumnName = "id")
    @Getter @Setter private Note note;
}
