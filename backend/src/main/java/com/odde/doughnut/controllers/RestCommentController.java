package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.Resource;

public class RestCommentController {
    private final ModelFactoryService modelFactoryService;
    private final CurrentUserFetcher currentUserFetcher;
    @Resource(name = "testabilitySettings")
    private final TestabilitySettings testabilitySettings;

    public RestCommentController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher, TestabilitySettings testabilitySettings) {
        this.modelFactoryService = modelFactoryService;
        this.currentUserFetcher = currentUserFetcher;
        this.testabilitySettings = testabilitySettings;
    }

    public void create(Note note, Comment newComment) {

    }

    @PostMapping(value = "/{comment}/delete")
    @Transactional
    public Integer deleteComment(@PathVariable("comment") Comment comment) throws NoAccessRightException {
        currentUserFetcher.getUser().getAuthorization().assertAuthorization(comment);
        modelFactoryService.toCommentModel(comment).destroy(testabilitySettings.getCurrentUTCTimestamp());
        modelFactoryService.entityManager.flush();
        return comment.getId();
    }

}
