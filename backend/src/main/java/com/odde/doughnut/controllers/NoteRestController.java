package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NoteRestController {
    private final CurrentUserFetcher currentUserFetcher;
    private final ModelFactoryService modelFactoryService;

    public NoteRestController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        this.currentUserFetcher = currentUserFetcher;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping(value = "/api/notes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<NoteEntity> getNotes() {
        List<NoteEntity> notes = currentUserFetcher.getUser().getNewNotesToReview();
        return notes;
    }

}
