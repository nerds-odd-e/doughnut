package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
public class UserEntity {

    @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @NotNull
    @Size(min = 1, max = 100)
    @Getter @Setter private String name;

    @Column(name = "external_identifier") @Getter @Setter private String externalIdentifier;

    @OneToMany(mappedBy = "userEntity")
    @JsonIgnore
    @Getter @Setter private List<NoteEntity> notes = new ArrayList<>();

    @OneToMany(mappedBy = "userEntity")
    @JsonIgnore
    @Getter @Setter private List<ReviewPointEntity> reviewPoints = new ArrayList<>();

    @OneToOne(mappedBy = "userEntity", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    @Getter @Setter private OwnershipEntity ownershipEntity = new OwnershipEntity();

    @Column(name = "daily_new_notes_count") @Getter @Setter private Integer dailyNewNotesCount = 15;

    @Pattern(regexp="^\\d+(,\\s*\\d+)*$",message="must be numbers separated by ','")
    @Column(name = "space_intervals") @Getter @Setter private String spaceIntervals = "0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55";

    @JoinTable(name = "circle_user", joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false)
    })
    @ManyToMany
    @JsonIgnore
    @Getter
    private final List<CircleEntity> circles = new ArrayList<>();

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.DETACH)
    @JsonIgnore
    @Getter
    private final List<SubscriptionEntity> subscriptionEntities = new ArrayList<>();

    public UserEntity() {
        ownershipEntity.setUserEntity(this);
    }

    public boolean owns(NoteEntity note) {
        return note.getOwnershipEntity().ownsBy(this);
    }

}
