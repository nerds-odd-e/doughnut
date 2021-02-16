package com.odde.doughnut.models;

import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "link")
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "source_id") @Setter private Integer sourceId;
    @Column(name = "target_id") @Setter private Integer targetId;
}
