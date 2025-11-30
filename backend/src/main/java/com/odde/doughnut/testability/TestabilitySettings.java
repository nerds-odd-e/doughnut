package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.utils.Randomizer;
import com.odde.doughnut.utils.randomizers.NonRandomizer;
import com.odde.doughnut.utils.randomizers.RealRandomizer;
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

  private static final String DISABLED_SENTINEL = "__DISABLED__";
  private final Map<String, String> replacedServiceUrls = new HashMap<>();
  private final Map<String, String> defaultServiceUrls =
      Map.of("wikidata", "https://www.wikidata.org", "openAi", "https://api.openai.com/v1/");

  public Timestamp timeTravelTo(Timestamp timestamp) {
    this.timestamp = timestamp;
    if (randomizer == null) {
      randomizer = new NonRandomizer();
    }
    return timestamp;
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
    return getServiceUrl("wikidata");
  }

  private String getServiceUrl(String serviceName) {
    if (this.replacedServiceUrls.containsKey(serviceName)) {
      String url = this.replacedServiceUrls.get(serviceName);
      if (DISABLED_SENTINEL.equals(url)) {
        return "";
      }
      return url;
    }
    return this.defaultServiceUrls.get(serviceName);
  }

  public void replaceServiceUrls(Map<String, String> setWikidataService) {
    setWikidataService.forEach(
        (key, value) -> {
          if (value == null || value.isEmpty()) {
            this.replacedServiceUrls.remove(key);
            return;
          }
          this.replacedServiceUrls.put(key, value);
        });
  }

  public String getOpenAiApiUrl() {
    return getServiceUrl("openAi");
  }

  public void disableOpenAi() {
    this.replacedServiceUrls.put("openAi", DISABLED_SENTINEL);
  }

  public boolean isOpenAiDisabled() {
    return this.replacedServiceUrls.containsKey("openAi")
        && DISABLED_SENTINEL.equals(this.replacedServiceUrls.get("openAi"));
  }

  void init() {
    timeTravelTo(null);
    setUseRealGithub(false);
    enableFeatureToggle(false);
    setRandomization(new Randomization(Randomization.RandomStrategy.first, 0));
    replacedServiceUrls.clear();
  }
}
