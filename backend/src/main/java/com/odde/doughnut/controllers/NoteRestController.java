package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class NoteRestController {
    private final CurrentUser currentUser;
    private final ModelFactoryService modelFactoryService;

    public NoteRestController(CurrentUser currentUser, ModelFactoryService modelFactoryService) {
        this.currentUser = currentUser;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping(value = "/api/notes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<NoteEntity> getNotes() {
        List<NoteEntity> notes = currentUser.getUser().getNewNotesToReview();
        return notes.stream().limit(currentUser.getUser().getDailyNewNotesCount()).collect(Collectors.toList());
    }

}
