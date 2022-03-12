package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAccessories;
import com.odde.doughnut.entities.TextContent;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NoteSphere {
    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    @JsonIgnore
    private Optional<Integer> parentId;

    @Getter
    @Setter
    @JsonIgnore
    private String title;

    @Getter
    @Setter
    @JsonIgnore
    private String shortDescription;

    @Getter
    @Setter
    @JsonIgnore
    private Optional<String> notePicture;

    @Getter
    @Setter
    @JsonIgnore
    private Timestamp createdAt;

    @Getter
    @Setter
    @JsonIgnore
    private NoteAccessories noteAccessories;

    @Getter
    @Setter
    private Map<Link.LinkType, LinkViewed> links;

    @Getter
    @Setter
    private List<Integer> childrenIds;

    @Getter
    @Setter
    @JsonIgnore
    private TextContent textContent;

    @Getter
    @Setter
    private Note note;

}
