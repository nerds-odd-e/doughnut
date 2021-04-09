package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "review_setting")
public class ReviewSetting {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name="remember_spelling")
    @Getter
    @Setter
    private Boolean rememberSpelling = false;
}
