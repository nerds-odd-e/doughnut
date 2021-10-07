
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.entities.json.NotesBulk;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
class RestNoteController {
    private final ModelFactoryService modelFactoryService;
    private final CurrentUserFetcher currentUserFetcher;
    @Resource(name = "testabilitySettings")
    private final TestabilitySettings testabilitySettings;

    public RestNoteController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher, TestabilitySettings testabilitySettings) {
        this.modelFactoryService = modelFactoryService;
        this.currentUserFetcher = currentUserFetcher;
        this.testabilitySettings = testabilitySettings;
    }

    class NoteStatistics {
        @Getter
        @Setter
        private ReviewPoint reviewPoint;
        @Getter
        @Setter
        private Note note;

    }

    static class NoteCreation {
        @Getter
        @Setter
        private Integer linkTypeToParent;
        @Getter
        @Setter
        @Valid
        @NotNull
        private NoteContent noteContent = new NoteContent();
    }

    @PostMapping(value = "/{parentNote}/create")
    @Transactional
    public RedirectToNoteResponse createNote(@PathVariable(name = "parentNote") Note parentNote, @Valid @ModelAttribute NoteCreation noteCreation) throws NoAccessRightException, IOException {
        final UserModel userModel = currentUserFetcher.getUser();
        userModel.getAuthorization().assertAuthorization(parentNote);
        User user = userModel.getEntity();
        Note note = Note.createNote(user, noteCreation.getNoteContent(), testabilitySettings.getCurrentUTCTimestamp());
        note.setParentNote(parentNote);
        modelFactoryService.noteRepository.save(note);
        if (noteCreation.getLinkTypeToParent() != null) {
            Link link = new Link();
            link.setUser(user);
            link.setSourceNote(note);
            link.setTargetNote(parentNote);
            link.setTypeId(noteCreation.getLinkTypeToParent());
            modelFactoryService.linkRepository.save(link);
        }
        return new RedirectToNoteResponse(note.getId());
    }

    @GetMapping("/{note}")
    public NotesBulk show(@PathVariable("note") Note note) throws NoAccessRightException {
        final UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertReadAuthorization(note);

        return NotesBulk.jsonNoteWithChildren(note, user);
    }

    @GetMapping("/{note}/overview")
    public NotesBulk showOverview(@PathVariable("note") Note note) throws NoAccessRightException {
        final UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertReadAuthorization(note);

        return NotesBulk.jsonNoteWitheDescendants(note, user);
    }

    @PostMapping(path = "/{note}")
    @Transactional
    public NoteViewedByUser updateNote(@PathVariable(name = "note") Note note, @Valid @ModelAttribute NoteContent noteContent) throws NoAccessRightException, IOException {
        final UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertAuthorization(note);
        noteContent.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
        note.updateNoteContent(noteContent, user.getEntity());
        modelFactoryService.noteRepository.save(note);
        return new NoteViewer(user.getEntity(), note).toJsonObject();
    }

    @GetMapping("/{note}/statistics")
    public NoteStatistics statistics(@PathVariable("note") Note note) throws NoAccessRightException {
        final UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertReadAuthorization(note);
        NoteStatistics statistics = new NoteStatistics();
        statistics.setReviewPoint(user.getReviewPointFor(note));
        statistics.setNote(note);
        return statistics;
    }

    @PostMapping("/{note}/search")
    @Transactional
    public List<Note> searchForLinkTarget(@PathVariable("note") Note note, @Valid @RequestBody SearchTerm searchTerm) {
        return currentUserFetcher.getUser().getLinkableNotes(note, searchTerm);
    }

    @PostMapping(value = "/{note}/delete")
    @Transactional
    public RedirectToNoteResponse deleteNote(@PathVariable("note") Note note) throws NoAccessRightException {
        currentUserFetcher.getUser().getAuthorization().assertAuthorization(note);
        Integer parentId = note.getParentId();
        modelFactoryService.toNoteModel(note).destroy();
        return new RedirectToNoteResponse(parentId);
    }

    @GetMapping("/{note}/review-setting")
    public ReviewSetting editReviewSetting(Note note) {
        ReviewSetting reviewSetting = note.getMasterReviewSetting();
        if (reviewSetting == null) {
            reviewSetting = new ReviewSetting();
        }
        return reviewSetting;
    }

    @PostMapping(value = "/{note}/review-setting")
    @Transactional
    public RedirectToNoteResponse updateReviewSetting(@PathVariable("note") Note note, @Valid @RequestBody ReviewSetting reviewSetting) throws NoAccessRightException {
        currentUserFetcher.getUser().getAuthorization().assertAuthorization(note);
        note.mergeMasterReviewSetting(reviewSetting);
        modelFactoryService.noteRepository.save(note);
        return new RedirectToNoteResponse(note.getId());
    }

    @PostMapping(value = "/{note}/split")
    @Transactional
    public RedirectToNoteResponse splitNote(@PathVariable("note") Note note) throws NoAccessRightException, IOException {
        final UserModel userModel = currentUserFetcher.getUser();
        userModel.getAuthorization().assertAuthorization(note);
        User user = userModel.getEntity();
        Integer parentId = note.getParentId();
        List<String> items = Arrays.asList(note.getNoteContent().getDescription().split("\n\n"));
        extractChildNotes(note, user, items);
        return new RedirectToNoteResponse(note.getId());
    }

    private void extractChildNotes(Note note, User user, List<String> items) {
        items.stream().filter(item -> !item.isBlank()).forEach(paragraph -> {
                    try {
                        Note childNote = createNoteFromParagraph(paragraph, user);
                        childNote.setParentNote(note);
                        modelFactoryService.noteRepository.save(childNote);
                    } catch (IOException e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                    }
                }
        );
    }

    private Note createNoteFromParagraph(String paragraph, User user) throws IOException {
        LinkedList<String> linesInParagraph = new LinkedList<String>(Arrays.stream(paragraph.split("\n")).toList());

        NoteContent childNoteContent = new NoteContent();
        childNoteContent.setTitle(linesInParagraph.getFirst());
        linesInParagraph.removeFirst();
        childNoteContent.setDescription(String.join("\n", linesInParagraph));

        Note childNote = Note.createNote(user, childNoteContent, testabilitySettings.getCurrentUTCTimestamp());
        return childNote;
    }

}
