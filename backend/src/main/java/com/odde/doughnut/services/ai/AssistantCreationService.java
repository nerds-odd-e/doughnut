package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.assistant.*;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service for creating and managing OpenAI Assistants using the Assistants API.
 *
 * <p>Creates assistant objects for notebook-specific AI features with file search capabilities.
 */
@Service
public class AssistantCreationService {
  protected final OpenAiApiHandler openAiApiHandler;
  protected final List<AiTool> tools;

  public AssistantCreationService(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    this.openAiApiHandler = new OpenAiApiHandler(openAiApi);
    this.tools = AiToolFactory.getAllAssistantTools();
  }

  public Assistant createDefaultAssistant(String modelName, String assistantName) {
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name(assistantName)
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(tools.stream().map(AiTool::getTool).toList())
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  private ToolResources uploadToolResources(String assistantName, String textContent)
      throws IOException {
    String fileId =
        openAiApiHandler.uploadTextFile(
            assistantName + ".json", textContent, "assistants", ".json");

    String vectorStoreId = openAiApiHandler.createVectorFile(assistantName, fileId);
    FileSearchResources fileSearchResources = new FileSearchResources();
    fileSearchResources.setVectorStoreIds(List.of(vectorStoreId));
    return new ToolResources(null, fileSearchResources);
  }

  public NotebookAssistant recreateNotebookAssistant(
      Timestamp currentUTCTimestamp, User creator, Notebook notebook, String modelName)
      throws IOException {
    String fileContent = notebook.getNotebookDump();
    String assistantName =
        "Assistant for notebook %s".formatted(notebook.getHeadNote().getTopicConstructor());
    ToolResources tooResources = uploadToolResources(assistantName, fileContent);
    List<Tool> toolList = new ArrayList<>(tools.stream().map(AiTool::getTool).toList());
    toolList.add(new FileSearchTool());
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name(assistantName)
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(toolList)
            .toolResources(tooResources)
            .build();
    Assistant notebookAssistant = openAiApiHandler.createAssistant(assistantRequest);
    return notebook.buildOrEditNotebookAssistant(
        currentUTCTimestamp, creator, notebookAssistant.getId());
  }
}
