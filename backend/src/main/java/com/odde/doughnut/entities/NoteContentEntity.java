package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class NoteContentEntity {
    @Getter @Setter private String description;

    @Getter
    @Setter
    private String url;

    @Column(name = "url_is_video")
    @Getter
    @Setter
    private Boolean urlIsVideo = false;

}
