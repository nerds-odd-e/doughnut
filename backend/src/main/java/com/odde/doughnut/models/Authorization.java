package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

public class Authorization {
    protected final User user;
    protected final ModelFactoryService modelFactoryService;

    public Authorization(User user, ModelFactoryService modelFactoryService) {
        this.user = user;
        this.modelFactoryService = modelFactoryService;
    }

    public void assertAuthorization(Note note) throws NoAccessRightException {
        if (!hasFullAuthority(note)) {
            throw new NoAccessRightException();
        }
    }

    public boolean hasFullAuthority(Note note) {
        return hasFullAuthority(note.getNotebook());
    }

    public boolean hasFullAuthority(Notebook notebook) {
        if(user == null) return false;
        return user.owns(notebook);
    }

    public boolean hasReferenceAuthority(Note note) {
        if (user == null) return false;
        return user.canReferTo(note.getNotebook());
    }

    public void assertReadAuthorization(Note note) throws NoAccessRightException {
        assertReadAuthorization(note.getNotebook());
    }

    public void assertReadAuthorization(Notebook notebook)
            throws NoAccessRightException {
        if (hasReferenceAuthority(notebook.getHeadNote())) {
            return;
        }
        if (modelFactoryService.bazaarNotebookRepository.findByNotebook(notebook) != null) {
            return;
        }
        modelFactoryService.toUserModel(user).getAuthorization().assertLoggedIn();
        throw new NoAccessRightException();
    }

    public void assertAuthorization(Notebook notebook)
            throws NoAccessRightException {
        if (!hasFullAuthority(notebook)) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(Circle circle) throws NoAccessRightException {
        if (!user.inCircle(circle)) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(Subscription subscription)
            throws NoAccessRightException {
        if (subscription.getUser() != user) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(User user) throws NoAccessRightException {
        if (!this.user.getId().equals(user.getId())) {
            throw new NoAccessRightException();
        }
    }

    public void assertAuthorization(Link link) throws NoAccessRightException {
        if (link.getUser().getId() != user.getId()) {
            throw new NoAccessRightException();
        }
    }

    public void assertDeveloperAuthorization() throws NoAccessRightException {
        if (!isDeveloper()) {
            throw new NoAccessRightException();
        }
    }

    private static final List<String> allowUsers =
            Arrays.asList("Terry", "t-machu", "Developer", "Yeong Sheng");

    public boolean isDeveloper() {
        if (user == null) return false;
        return allowUsers.contains(user.getName());
    }

    public void assertLoggedIn() {
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User Not Found");
        }
    }

    public void assertAuthorization(ReviewPoint reviewPoint) {

    }
}
