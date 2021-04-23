
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BlogModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/blog_articles_by_website_name/{websiteName}")
    public List<BlogArticle> getBlogArticlesByWebsiteName(@PathVariable String websiteName) {
        Note headNote = modelFactoryService.noteRepository.findFirstByTitle(websiteName);
        List<NotesClosure> noteClosures = headNote.getDescendantNCs();
        List<NotesClosure> filteredClosures = noteClosures.stream().filter(closure -> closure.getDepth() >= 4).collect(Collectors.toList());
        return filteredClosures.stream().map(x -> x.getNote().toBlogArticle()).collect(Collectors.toList());
    }

    @GetMapping("/blog/yearmonth")
    public List<BlogYearMonth> getBlogYearMonthList() {
        Note note = modelFactoryService.noteRepository.findFirstByTitle("odd-e-blog");
        BlogModel blogModel = modelFactoryService.toBlogModel(note);
        List<BlogYearMonth> yearMonths = blogModel.getBlogYearMonths(note);

        return yearMonths;
    }
}
