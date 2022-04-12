package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class Comment {
    private Integer id;
    private User author;
    private Timestamp createdAt;
    private String description;
}

