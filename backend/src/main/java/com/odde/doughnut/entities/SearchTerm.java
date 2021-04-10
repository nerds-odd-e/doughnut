package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

public class SearchTerm {
    @Getter
    @Setter
    private String searchKey = "";
    @Getter
    @Setter
    private Boolean searchGlobally = false;

    public SearchTerm() {
    }
}
