package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "link")
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "source_id") @Getter @Setter private Integer sourceId;
    @Column(name = "target_id") @Getter @Setter private Integer targetId;
}
