package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.LinkModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/links")
public class LinkController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public LinkController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/{link}")
    public String show(@PathVariable("link") Link link, Model model) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(link);
        return "links/show";
    }

    @GetMapping("/{note}/link")
    public String link(@PathVariable("note") Note note, @RequestParam(required = false) String searchTerm, Model model) {
        List<Note> linkableNotes = currentUserFetcher.getUser().filterLinkableNotes(note, searchTerm);
        model.addAttribute("linkableNotes", linkableNotes);
        return "links/new";
    }

    @PostMapping(value = "/{note}/link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String linkNote(@PathVariable("note") Note note, Integer targetNoteId, Model model) throws NoAccessRightException {
        Note targetNote = modelFactoryService.noteRepository.findById(targetNoteId).get();
        Link link = new Link();
        link.setSourceNote(note);
        link.setTargetNote(targetNote);
        link.setType(Link.LinkType.RELATED_TO.label);
        model.addAttribute("link", link);
        return "links/link_choose_type";
    }

    @PostMapping(value = "/create_link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String linkNoteFinalize(@Valid Link link, BindingResult bindingResult) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "links/link_choose_type";
        }
        currentUserFetcher.getUser().assertAuthorization(link.getSourceNote());
        link.setUserEntity(currentUserFetcher.getUser().getEntity());
        modelFactoryService.linkRepository.save(link);
        return "redirect:/notes/" + link.getSourceNote().getId();
    }

    @PostMapping(value = "/{link}", params="submit", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updateLink(@Valid Link link, BindingResult bindingResult) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "links/show";
        }
        currentUserFetcher.getUser().assertAuthorization(link.getSourceNote());
        modelFactoryService.linkRepository.save(link);
        return "redirect:/notes/" + link.getSourceNote().getId();
    }

    @PostMapping(value = "/{link}", params="delete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String deleteLink(@Valid Link link) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(link.getSourceNote());
        LinkModel linkModel = modelFactoryService.toLinkModel(link);
        linkModel.destroy();
        return "redirect:/notes/" + link.getSourceNote().getId();
    }

}
