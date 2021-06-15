package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Link;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class LinkViewedByUser {
    @Getter
    @Setter
    @JsonIgnoreProperties("sourceNote")
    private List<Link> direct;
    @Getter
    @Setter
    @JsonIgnoreProperties("targetNote")
    private List<Link> reverse;
}
