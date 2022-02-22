package com.odde.doughnut.controllers;

import com.odde.doughnut.models.MessageModel;
import com.odde.doughnut.models.NoteModel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {

  @MessageMapping("/getUpdates")
  @SendTo("/realtimeUpdate")
  public MessageModel getUpdate(NoteModel note){
    return new MessageModel("test");
  }

}