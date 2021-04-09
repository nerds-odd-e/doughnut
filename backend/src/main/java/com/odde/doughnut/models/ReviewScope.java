package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;

import java.util.List;

public interface ReviewScope {
    List<Note> getNotesHaveNotBeenReviewedAtAll();

    int getNotesHaveNotBeenReviewedAtAllCount();

    List<Link> getLinksHaveNotBeenReviewedAtAll();
}
