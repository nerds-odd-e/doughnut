package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.TextContent;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class NoteCreation {
    @Getter
    @Setter
    public Integer linkTypeToParent;
    @Getter
    @Setter
    @Valid
    @NotNull
    public TextContent textContent = new TextContent();
}
