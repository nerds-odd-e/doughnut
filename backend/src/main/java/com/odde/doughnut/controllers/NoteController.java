package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

@Controller
@RequestMapping("/notes")
public class NoteController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public NoteController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/{parentNote}/new")
    public String newNote(@PathVariable(name = "parentNote") Note parentNote, Model model) throws NoAccessRightException {
        UserModel userModel = getCurrentUser();
        userModel.getAuthorization().assertAuthorization(parentNote);
        NoteContent noteContent = new NoteContent();
        model.addAttribute("ownership", userModel.getEntity().getOwnership());
        model.addAttribute("noteContent", noteContent);
        return "notes/new";
    }

    @PostMapping("/{parentNote}/create")
    public String createNote(@PathVariable(name = "parentNote") Note parentNote, @Valid NoteContent noteContent, BindingResult bindingResult) throws NoAccessRightException, IOException {
        if (bindingResult.hasErrors()) {
            return "notes/new";
        }
        UserModel userModel = getCurrentUser();
        userModel.getAuthorization().assertAuthorization(parentNote);
        User user = userModel.getEntity();

        if (parentNote.getNotebook().getNotebookType().equals(NotebookType.BLOG)) {
            parentNote = injectingYearAndMonth(parentNote, user);
        }

        Note note = new Note();
        note.updateNoteContent(noteContent, user);
        note.setParentNote(parentNote);
        note.setUser(user);
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    private Note injectingYearAndMonth(Note parentNote, User user) throws IOException {
        LocalDate now = LocalDate.now();
        int year = now.getYear();

        int day = now.getDayOfMonth();
        String yearNoteTitle = String.valueOf(year);
        String monthNoteTitle = now.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String dayNoteTitle = String.valueOf(day);

        Note yearNote = getOrCreateNoteInNotebook(parentNote, user, yearNoteTitle);

        Note monthNote = getOrCreateNoteInNotebook(yearNote, user, monthNoteTitle);

        return getOrCreateNoteInNotebook(monthNote, user, dayNoteTitle);
    }

    private Note getOrCreateNoteInNotebook(Note parentNote, User user, String noteTitle) throws IOException {
        Note existingNote = parentNote.getNotebook().getNotes().stream().filter(note -> note.getTitle().equals(noteTitle)).findFirst().orElse(null);
        if(existingNote != null){
            return existingNote;
        }

        NoteContent noteContent = new NoteContent();
        noteContent.setTitle(noteTitle);
        Note note = new Note();
        note.updateNoteContent(noteContent, user);
        note.setParentNote(parentNote);
        note.setUser(user);
        modelFactoryService.noteRepository.save(note);
        return note;
    }


    @GetMapping("/{note}")
    public String showNote(@PathVariable(name = "note") Note note) throws NoAccessRightException {
        getCurrentUser().getAuthorization().assertReadAuthorization(note);
        if (!getCurrentUser().getAuthorization().hasFullAuthority(note)) {
            return "redirect:/bazaar/notes/" + note.getId();
        }
        return "notes/show";
    }

    @GetMapping("/articles/{note}")
    public String showNoteAsArticle(@PathVariable(name = "note") Note note) throws NoAccessRightException {
        getCurrentUser().getAuthorization().assertAuthorization(note);
        return "notes/article";
    }

    @GetMapping("/{note}/edit")
    public String editNote(Note note, Model model) {
        model.addAttribute("noteContent", note.getNoteContent());
        return "notes/edit";
    }

    @PostMapping("/{note}")
    public String updateNote(@PathVariable(name = "note") Note note, @Valid NoteContent noteContent, BindingResult bindingResult) throws NoAccessRightException, IOException {
        getCurrentUser().getAuthorization().assertAuthorization(note);
        if (bindingResult.hasErrors()) {
            return "notes/edit";
        }
        note.updateNoteContent(noteContent, getCurrentUser().getEntity());
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{note}/move")
    public String prepareToMove(Note note, Model model) {
        model.addAttribute("noteMotion", getLeftNoteMotion(note));
        model.addAttribute("noteMotionRight", getRightNoteMotion(note));
        model.addAttribute("noteMotionUnder", new NoteMotion(null, true));
        return "notes/move";
    }

    private NoteMotion getLeftNoteMotion(Note note) {
        Note previousSiblingNote = note.getPreviousSibling();
        if(previousSiblingNote != null) {
            Note prevprev = previousSiblingNote.getPreviousSibling();
            if (prevprev == null) {
                return new NoteMotion(note.getParentNote(), true);
            }
            return new NoteMotion(prevprev, false);
        }
        return new NoteMotion(null, false);
    }

    private NoteMotion getRightNoteMotion(Note note) {
        return new NoteMotion(note.getNextSibling(), false);
    }

    @PostMapping("/{note}/move")
    @Transactional
    public String moveNote(Note note, NoteMotion noteMotion) throws CyclicLinkDetectedException, NoAccessRightException {
        getCurrentUser().getAuthorization().assertAuthorization(note);
        getCurrentUser().getAuthorization().assertAuthorization(noteMotion.getRelativeToNote());
        modelFactoryService.toNoteMotionModel(noteMotion, note).execute();
        return "redirect:/notes/" + note.getId();
    }

    @PostMapping(value = "/{note}/delete")
    @Transactional
    public RedirectView deleteNote(@PathVariable("note") Note note) throws NoAccessRightException {
        getCurrentUser().getAuthorization().assertAuthorization(note);
        modelFactoryService.toTreeNodeModel(note).destroy();
        return new RedirectView("/notebooks");
    }

    @GetMapping("/{note}/review_setting")
    public String editReviewSetting(Note note, Model model) {
        ReviewSetting reviewSetting = note.getMasterReviewSetting();
        if(reviewSetting == null) {
            reviewSetting = new ReviewSetting();
        }
        model.addAttribute("reviewSetting", reviewSetting);
        return "notes/edit_review_setting";
    }

    @PostMapping(value = "/{note}/review_setting")
    @Transactional
    public String updateReviewSetting(@PathVariable("note") Note note, @Valid ReviewSetting reviewSetting, BindingResult bindingResult) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "notes/edit_review_setting";
        }
        getCurrentUser().getAuthorization().assertAuthorization(note);
        note.mergeMasterReviewSetting(reviewSetting);
        modelFactoryService.noteRepository.save(note);

        return "redirect:/notes/" + note.getId();
    }

    private UserModel getCurrentUser() {
        return currentUserFetcher.getUser();
    }
}
