package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ownership")
public class OwnershipEntity {
    @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @Getter @Setter private UserEntity userEntity;

    @OneToOne
    @JoinColumn(name = "circle_id")
    @JsonIgnore
    @Getter @Setter private CircleEntity circleEntity;

    @OneToMany(mappedBy = "ownershipEntity")
    @Where(clause = "parent_id IS NULL")
    @JsonIgnore
    @Getter private List<NoteEntity> orphanedNotes;

}
