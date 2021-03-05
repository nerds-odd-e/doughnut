package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RestController
public class CheapBackupController {
    private final CurrentUserFetcher currentUserFetcher;
    private final ModelFactoryService modelFactoryService;

    public CheapBackupController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        this.currentUserFetcher = currentUserFetcher;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/api/backup")
    public HashMap<String, Object> myNotes(Model model) {
        HashMap<String, Object> hash = new HashMap<>();
        hash.put("users", modelFactoryService.userRepository.findAll());
        hash.put("notes", modelFactoryService.noteRepository.findAll());
        hash.put("review_points", modelFactoryService.reviewPointRepository.findAll());
        return hash;
    }
}

