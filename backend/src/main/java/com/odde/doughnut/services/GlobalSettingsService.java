package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.GlobalAiModelSettings;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class GlobalSettingsService {

  public static final String DEFAULT_CHAT_MODEL = "gpt-3.5-turbo";
  private final ModelFactoryService modelFactoryService;

  public GlobalSettingsService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public GlobalSettingsKeyValue getGlobalSettingQuestionGeneration() {
    return new GlobalSettingsKeyValue(
        "question_generation_model", DEFAULT_CHAT_MODEL, modelFactoryService);
  }

  public GlobalSettingsKeyValue getGlobalSettingEvaluation() {
    return new GlobalSettingsKeyValue("evaluation_model", DEFAULT_CHAT_MODEL, modelFactoryService);
  }

  public GlobalSettingsKeyValue getGlobalSettingOthers() {
    return new GlobalSettingsKeyValue("others_model", DEFAULT_CHAT_MODEL, modelFactoryService);
  }

  public GlobalSettingsKeyValue getNoteCompletionAssistantId() {
    return new GlobalSettingsKeyValue(
        "note_completion_assistant", "asst_37nHzDavC0gLbxydvprHwoca", modelFactoryService);
  }

  public record GlobalSettingsKeyValue(
      String keyName, String defaultValue, ModelFactoryService modelFactoryService) {
    public String getValue() {
      return getGlobalSettings().getValue();
    }

    public void setKeyValue(Timestamp currentUTCTimestamp, String value) {
      GlobalSettings settings = getGlobalSettings();
      settings.setValue(value);
      settings.setUpdatedAt(currentUTCTimestamp);
      if (settings.getId() == null) {
        modelFactoryService.createRecord(settings);
      } else {
        modelFactoryService.updateRecord(settings);
      }
    }

    public Timestamp getCreatedAt() {
      return getGlobalSettings().getUpdatedAt();
    }

    private GlobalSettings getGlobalSettings() {
      GlobalSettings currentQuestionGenerationModelVersion =
          modelFactoryService.globalSettingRepository.findByKeyName(keyName);
      if (currentQuestionGenerationModelVersion == null) {
        GlobalSettings globalSettings = new GlobalSettings();
        globalSettings.setKeyName(keyName);
        globalSettings.setValue(defaultValue);
        return globalSettings;
      }
      return currentQuestionGenerationModelVersion;
    }
  }

  public GlobalAiModelSettings getCurrentModelVersions() {
    return new GlobalAiModelSettings(
        getGlobalSettingQuestionGeneration().getValue(),
        getGlobalSettingEvaluation().getValue(),
        getGlobalSettingOthers().getValue());
  }

  public GlobalAiModelSettings setCurrentModelVersions(
      GlobalAiModelSettings models, Timestamp currentUTCTimestamp) {
    getGlobalSettingQuestionGeneration()
        .setKeyValue(currentUTCTimestamp, models.getQuestionGenerationModel());
    getGlobalSettingEvaluation().setKeyValue(currentUTCTimestamp, models.getEvaluationModel());
    getGlobalSettingOthers().setKeyValue(currentUTCTimestamp, models.getOthersModel());
    return models;
  }
}
