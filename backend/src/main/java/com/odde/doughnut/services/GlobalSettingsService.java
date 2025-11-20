package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.GlobalAiModelSettings;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.entities.repositories.GlobalSettingRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class GlobalSettingsService {

  public static final String DEFAULT_CHAT_MODEL = "gpt-4o-mini";
  private final GlobalSettingRepository globalSettingRepository;
  private final EntityPersister entityPersister;

  public GlobalSettingsService(
      GlobalSettingRepository globalSettingRepository, EntityPersister entityPersister) {
    this.globalSettingRepository = globalSettingRepository;
    this.entityPersister = entityPersister;
  }

  public GlobalSettingsKeyValue globalSettingQuestionGeneration() {
    return new GlobalSettingsKeyValue(
        "question_generation_model", DEFAULT_CHAT_MODEL, globalSettingRepository, entityPersister);
  }

  public GlobalSettingsKeyValue globalSettingEvaluation() {
    return new GlobalSettingsKeyValue(
        "evaluation_model", DEFAULT_CHAT_MODEL, globalSettingRepository, entityPersister);
  }

  public GlobalSettingsKeyValue globalSettingOthers() {
    return new GlobalSettingsKeyValue(
        "others_model", DEFAULT_CHAT_MODEL, globalSettingRepository, entityPersister);
  }

  public static class GlobalSettingsKeyValue implements SettingAccessor {
    private final String keyName;
    private final String defaultValue;
    private final GlobalSettingRepository globalSettingRepository;
    private final EntityPersister entityPersister;

    public GlobalSettingsKeyValue(
        String keyName,
        String defaultValue,
        GlobalSettingRepository globalSettingRepository,
        EntityPersister entityPersister) {
      this.keyName = keyName;
      this.defaultValue = defaultValue;
      this.globalSettingRepository = globalSettingRepository;
      this.entityPersister = entityPersister;
    }

    @Override
    public String getValue() {
      return getGlobalSettings().getValue();
    }

    @Override
    public void setKeyValue(Timestamp currentUTCTimestamp, String value) {
      GlobalSettings settings = getGlobalSettings();
      settings.setValue(value);
      settings.setUpdatedAt(currentUTCTimestamp);
      entityPersister.save(settings);
    }

    public Timestamp getCreatedAt() {
      return getGlobalSettings().getUpdatedAt();
    }

    private GlobalSettings getGlobalSettings() {
      GlobalSettings currentQuestionGenerationModelVersion =
          globalSettingRepository.findByKeyName(keyName);
      if (currentQuestionGenerationModelVersion == null) {
        GlobalSettings globalSettings = new GlobalSettings();
        globalSettings.setKeyName(keyName);
        globalSettings.setValue(defaultValue);
        return globalSettings;
      }
      return currentQuestionGenerationModelVersion;
    }

    public String keyName() {
      return keyName;
    }
  }

  public GlobalAiModelSettings getCurrentModelVersions() {
    return new GlobalAiModelSettings(
        globalSettingQuestionGeneration().getValue(),
        globalSettingEvaluation().getValue(),
        globalSettingOthers().getValue());
  }

  public GlobalAiModelSettings setCurrentModelVersions(
      GlobalAiModelSettings models, Timestamp currentUTCTimestamp) {
    globalSettingQuestionGeneration()
        .setKeyValue(currentUTCTimestamp, models.getQuestionGenerationModel());
    globalSettingEvaluation().setKeyValue(currentUTCTimestamp, models.getEvaluationModel());
    globalSettingOthers().setKeyValue(currentUTCTimestamp, models.getOthersModel());
    return models;
  }
}
