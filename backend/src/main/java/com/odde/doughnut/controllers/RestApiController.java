
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:8000")
@RequestMapping("/api")
class RestApiController {
    @Autowired
    private Environment environment;

    private final ModelFactoryService modelFactoryService;

    public RestApiController(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/healthcheck")
    public String ping() {
        return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
    }

    @GetMapping("/bazaar_notes")
    public List<Notebook> getBazaarNotes() {
        BazaarModel bazaarModel = modelFactoryService.toBazaarModel();
        return bazaarModel.getAllNotebooks();
    }

    @GetMapping("/note/blog")
    public Note.NoteApiResult getNote() {
        Note note = modelFactoryService.noteRepository.findFirstByTitle("odd-e blog");
        Note targetNote = note.getChildren().stream().findFirst().orElse(new Note());

        Note.NoteApiResult result = new Note.NoteApiResult();
        result.setTitle(targetNote.getTitle());
        result.setDescription(targetNote.getArticleBody());
        result.setAuthor(targetNote.getUser().getName());
        result.setUpdateDatetime(targetNote.getNoteContent().getUpdatedDatetime().toString());

        return result;
    }
}
