package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/circles")
public class CircleController {
    private final CurrentUserFetcher currentUserFetcher;
    private final ModelFactoryService modelFactoryService;


    public CircleController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        this.currentUserFetcher = currentUserFetcher;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String index(Model model) {
        UserModel user = currentUserFetcher.getUser();
        model.addAttribute("userEntity", user.getEntity());
        model.addAttribute("circleJoiningByInvitationEntity", new CircleJoiningByInvitationEntity());
        return "circles/index";
    }

    @GetMapping("/new")
    public String newCircle(Model model) {
        model.addAttribute("circleEntity", new CircleEntity());
        return "circles/new";
    }

    @PostMapping("")
    public String createCircle(@Valid CircleEntity circleEntity, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "circles/new";
        }
        UserModel userModel = currentUserFetcher.getUser();
        CircleModel circleModel = modelFactoryService.toCircleModel(circleEntity);
        circleModel.joinAndSave(userModel);
        return "redirect:/circles/" + circleEntity.getId();
    }

    @GetMapping("/{circleEntity}")
    public String showCircle(@PathVariable("circleEntity") CircleEntity circleEntity) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(circleEntity);
        return "circles/show";
    }

    @PostMapping("/join")
    @Transactional
    public String joinCircle(@Valid CircleJoiningByInvitationEntity circleJoiningByInvitationEntity, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "circles/join";
        }
        CircleModel circleModel = modelFactoryService.findCircleByInvitationCode(circleJoiningByInvitationEntity.getInvitationCode());
        if (circleModel == null) {
            bindingResult.rejectValue("invitationCode", "error.error", "Does not match any circle");
            return "circles/join";
        }
        UserModel userModel = currentUserFetcher.getUser();
        if (userModel.inCircle(circleModel.getEntity())) {
            bindingResult.rejectValue("invitationCode", "error.error", "You are already in this circle");
            return "circles/join";
        }
        circleModel.joinAndSave(userModel);
        return "redirect:/circles/" + circleModel.getEntity().getId();
    }

    @GetMapping("/{circleEntity}/notes/new")
    public String newNoteInCircle(@PathVariable("circleEntity") CircleEntity circleEntity, Model model) {
        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setOwnershipEntity(circleEntity.getOwnershipEntity());
        model.addAttribute("noteEntity", noteEntity);
        return "notes/new";
    }

}
