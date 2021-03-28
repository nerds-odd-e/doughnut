package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.security.Principal;

@Controller
@RequestMapping("/users")
public class UserController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public UserController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @PostMapping("")
    public RedirectView createUser(Principal principal, UserEntity userEntity) {
        userEntity.setExternalIdentifier(principal.getName());
        modelFactoryService.userRepository.save(userEntity);
        return new RedirectView("/");
    }

    @GetMapping("/edit")
    public String editUser(Model model) {
        model.addAttribute("userEntity", currentUserFetcher.getUser().getEntity());
        return "users/edit";
    }

    @PostMapping("/{userEntity}")
    public String updateUser(@Valid UserEntity userEntity, BindingResult bindingResult) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(userEntity);
        if (bindingResult.hasErrors()) {
            return "users/edit";
        }
        modelFactoryService.userRepository.save(userEntity);
        return "redirect:/";
    }

}
