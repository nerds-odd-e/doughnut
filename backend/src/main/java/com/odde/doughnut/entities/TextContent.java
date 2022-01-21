package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "text_content")
public class TextContent {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String description;

    @Column(name = "updated_at")
    @Getter
    @Setter
    private Timestamp updatedAt;

}

