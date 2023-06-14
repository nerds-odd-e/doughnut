package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

import javax.xml.stream.events.Comment;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/comment")
record RestCommentController(ModelFactoryService modelFactoryService) {
  @GetMapping("/{noteId}")
  public List<Comment> getComments(@PathVariable(name = "noteId") int noteId) {
    return new ArrayList<Comment>();
  }

}
