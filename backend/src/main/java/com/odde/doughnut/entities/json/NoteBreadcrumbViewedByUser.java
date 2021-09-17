package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class NoteBreadcrumbViewedByUser {

    @Getter
    @Setter
    @JsonIgnoreProperties({"headNote"})
    private Notebook notebook;
    @Getter
    @Setter
    @JsonIgnoreProperties({"noteContent"})
    private List<Note> ancestors;
    @Getter
    @Setter
    private Boolean owns;
}
