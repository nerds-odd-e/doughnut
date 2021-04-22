package com.odde.doughnut.entities;

public enum NotebookType {
    GENERAL, BLOG;

    String getDisplay() {
        return this == BLOG ? "Article" : "Child Note";
    }
}
