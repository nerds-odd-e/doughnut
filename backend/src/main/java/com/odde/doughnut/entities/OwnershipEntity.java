package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;

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

    public boolean ownsBy(UserEntity user) {
        if(userEntity != null) {
            return userEntity.equals(user);
        }
        return circleEntity.getMembers().contains(user);
    }

    public boolean isFromCircle() { return circleEntity != null; }

}
