package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comment")
record RestCommentController(ModelFactoryService modelFactoryService) {
  @GetMapping("/{noteId}")
  public List<Comment> getComments(@PathVariable(name = "noteId") int noteId) {
    return List.of(new Comment(1, "Taiwan #1", noteId, new Timestamp(System.currentTimeMillis())));
  }
}
