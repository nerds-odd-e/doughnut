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
import java.util.Optional;

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
        Note note = new Note();
        note.updateNoteContent(noteContent, user);
        note.setParentNote(parentNote);
        note.setUser(user);
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }


    @GetMapping("/{note}")
    public String showNote(@PathVariable(name = "note") Integer noteId) {
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
        return note.getPreviousSibling()
                .map(prev->
                    prev.getPreviousSibling()
                            .map(n-> new NoteMotion(n, false))
                            .orElseGet(()->new NoteMotion(note.getParentNote(), true))
                ).orElseGet(()->new NoteMotion(null, false));
    }

    private NoteMotion getRightNoteMotion(Note note) {
        return new NoteMotion(note.getNextSibling().orElse(null), false);
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
        modelFactoryService.toNoteModel(note).destroy();
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
