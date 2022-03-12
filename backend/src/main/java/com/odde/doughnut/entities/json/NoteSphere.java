package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class NoteSphere {
    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    private Map<Link.LinkType, LinkViewed> links;

    @Getter
    @Setter
    private List<Integer> childrenIds;

    @Getter
    @Setter
    private Note note;

}
