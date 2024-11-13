package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NotebookAssistantCreationParams;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAssistantFacade;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.assistant.Assistant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/assistant")
public class RestAiAssistantCreationController {
  private final UserModel currentUser;
  private final ModelFactoryService modelFactoryService;
  private final AiAssistantFacade aiAssistantFacade;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestAiAssistantCreationController(
      ModelFactoryService modelFactoryService,
      AiAssistantFacade aiAssistantFacade,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.aiAssistantFacade = aiAssistantFacade;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping("/recreate-default")
  @Transactional
  public Map<String, String> recreateDefaultAssistant() throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    Assistant assistant = aiAssistantFacade.recreateDefaultAssistant(currentUTCTimestamp);
    Map<String, String> result = new HashMap<>();
    result.put(assistant.getName(), assistant.getId());
    return result;
  }

  @PostMapping("/recreate-notebook/{notebook}")
  @Transactional
  public NotebookAssistant recreateNotebookAssistant(
      @PathVariable(value = "notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody NotebookAssistantCreationParams notebookAssistantCreationParams)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    NotebookAssistant notebookAssistant =
        aiAssistantFacade.recreateNotebookAssistant(
            currentUTCTimestamp,
            currentUser.getEntity(),
            notebook,
            notebookAssistantCreationParams.getAdditionalInstruction());
    this.modelFactoryService.save(notebookAssistant);
    return notebookAssistant;
  }
}
