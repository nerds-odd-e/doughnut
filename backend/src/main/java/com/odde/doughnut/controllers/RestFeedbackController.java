package com.odde.doughnut.controllers;


import com.odde.doughnut.controllers.dto.FeedbackDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class RestFeedbackController {

  @PostMapping("/sendFeedback")
  public ResponseEntity<String> sendFeedback(@RequestBody FeedbackDTO feedbackDTO) {
    return ResponseEntity.ok("Feedback received successfully!");
  }



}
