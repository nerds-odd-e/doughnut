package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

public class BlogPost {

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private String author;

    @Getter
    @Setter
    private String createdDatetime;
}
