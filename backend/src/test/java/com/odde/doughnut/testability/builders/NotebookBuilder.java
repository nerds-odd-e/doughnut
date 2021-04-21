package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.MakeMe;

public class NotebookBuilder {
    private MakeMe makeMe;

    public NotebookBuilder(MakeMe makeMe) {
        this.makeMe = makeMe;
    }

    public Notebook please() {
        return makeMe.aNote().please().getNotebook();
    }
}
