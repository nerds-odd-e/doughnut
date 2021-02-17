package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "link")
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "source_id", referencedColumnName = "id")
    @Getter @Setter private Note sourceNote;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "target_id", referencedColumnName = "id")
    @Getter @Setter private Note targetNote;
}
