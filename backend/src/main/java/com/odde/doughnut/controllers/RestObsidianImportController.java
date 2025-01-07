package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class RestObsidianImportController {
    private final ModelFactoryService modelFactoryService;

    public RestObsidianImportController(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @PostMapping("/obsidian/{parentNoteId}/import")
    public NoteRealm importObsidianNotes(MultipartFile file, @PathVariable Integer parentNoteId) throws UnexpectedNoAccessRightException, IOException {
        // Implementation will be added later
        return null;
    }
} 