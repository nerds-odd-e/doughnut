package com.odde.doughnut.controllers;

import com.odde.doughnut.configs.AccountAwareUrlAuthenticationSuccessHandler;
import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;

@Controller
@RequestMapping("/users")
public class UserController extends ApplicationMvcController  {
    private final AccountAwareUrlAuthenticationSuccessHandler accountAwareUrlAuthenticationSuccessHandler;
    private final ModelFactoryService modelFactoryService;

    public UserController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher, AccountAwareUrlAuthenticationSuccessHandler accountAwareUrlAuthenticationSuccessHandler) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
        this.accountAwareUrlAuthenticationSuccessHandler = accountAwareUrlAuthenticationSuccessHandler;
    }

    @PostMapping("")
    public void createUser(Principal principal, User user, HttpServletRequest request, HttpServletResponse resp, Authentication auth) throws ServletException, IOException {
        user.setExternalIdentifier(principal.getName());
        modelFactoryService.userRepository.save(user);
        accountAwareUrlAuthenticationSuccessHandler.onAuthenticationSuccess(request, resp, auth);
    }

    @GetMapping("/edit")
    public String editUser(Model model) {
        model.addAttribute("user", currentUserFetcher.getUser().getEntity());
        return "users/edit";
    }

    @PostMapping("/{user}")
    public String updateUser(@Valid User user, BindingResult bindingResult) throws NoAccessRightException {
        currentUserFetcher.getUser().getAuthorization().assertAuthorization(user);
        if (bindingResult.hasErrors()) {
            return "users/edit";
        }
        modelFactoryService.userRepository.save(user);
        return "redirect:/";
    }

}
