package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.GithubService;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

@Component
@ApplicationScope
public class TestabilitySettings {
  private Timestamp timestamp = null;
  private Randomizer randomizer = null;
  @Getter @Setter Boolean useRealGithub = true;
  @Autowired GithubService githubService;
  @Getter private boolean featureToggleEnabled = false;

  private Map<String, String> serviceUrls =
      new HashMap<>() {
        {
          put("wikidata", "https://www.wikidata.org");
          put("openAi", "https://api.openai.com/v1/");
        }
      };

  public void timeTravelTo(Timestamp timestamp) {
    this.timestamp = timestamp;
    if (randomizer == null) {
      randomizer = new NonRandomizer();
    }
  }

  public Timestamp getCurrentUTCTimestamp() {
    if (timestamp == null) {
      return new Timestamp(System.currentTimeMillis());
    }
    return timestamp;
  }

  public Randomizer getRandomizer() {
    if (randomizer == null) {
      return new RealRandomizer();
    }
    return randomizer;
  }

  public void setRandomization(Randomization option) {
    if (option.choose == Randomization.RandomStrategy.seed) {
      randomizer = new RealRandomizer(option.seed);
      return;
    }
    NonRandomizer nonRandomizer = new NonRandomizer();
    nonRandomizer.setAlwaysChoose(option.choose);
    randomizer = nonRandomizer;
  }

  public GithubService getGithubService() {
    if (useRealGithub) {
      return githubService;
    }
    return new NullGithubService();
  }

  public void enableFeatureToggle(boolean enabled) {
    this.featureToggleEnabled = enabled;
  }

  public String getWikidataServiceUrl() {
    return this.serviceUrls.get("wikidata");
  }

  public Map<String, String> replaceServiceUrls(Map<String, String> setWikidataService) {
    HashMap<String, String> saved = new HashMap<>();
    replaceServiceUrl(setWikidataService, saved, "wikidata");
    replaceServiceUrl(setWikidataService, saved, "openAi");
    return saved;
  }

  private void replaceServiceUrl(
      Map<String, String> setWikidataService, HashMap<String, String> saved, String serviceName) {
    if (setWikidataService.containsKey(serviceName)) {
      saved.put(serviceName, this.serviceUrls.get(serviceName));
      this.serviceUrls.put(serviceName, setWikidataService.get(serviceName));
    }
  }

  public String getOpenAiApiUrl() {
    return this.serviceUrls.get("openAi");
  }
}
