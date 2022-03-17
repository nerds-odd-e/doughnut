package com.odde.doughnut.entities.json;

import javax.validation.constraints.NotNull;

public class LinkRequest {
    @NotNull
    public Integer typeId;
    public Boolean moveUnder;
    public Boolean asFirstChild;
}
