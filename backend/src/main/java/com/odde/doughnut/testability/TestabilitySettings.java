package com.odde.doughnut.testability;

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
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class TestabilitySettings {
  private Timestamp timestamp = null;
  private NonRandomizer nonRandomizer = null;
  @Getter @Setter Boolean useRealGithub = true;
  @Autowired GithubService githubService;
  @Getter private boolean featureToggleEnabled = false;
  private Map<String, String> serviceUrls =
      new HashMap<>() {
        {
          put("wikidata", "https://www.wikidata.org");
          put("openai", "https://api.openai.com/");
        }
      };

  public void timeTravelTo(Timestamp timestamp) {
    this.timestamp = timestamp;
    if (nonRandomizer == null) {
      nonRandomizer = new NonRandomizer();
    }
  }

  public Timestamp getCurrentUTCTimestamp() {
    if (timestamp == null) {
      return new Timestamp(System.currentTimeMillis());
    }
    return timestamp;
  }

  public Randomizer getRandomizer() {
    if (nonRandomizer == null) {
      return new RealRandomizer();
    }
    return nonRandomizer;
  }

  public void setAlwaysChoose(String option) {
    if (nonRandomizer == null) {
      nonRandomizer = new NonRandomizer();
    }
    nonRandomizer.setAlwaysChoose(option);
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

  public Map<String, String> replaceServiceUrl(Map<String, String> setWikidataService) {
    HashMap<String, String> saved = new HashMap<>();
    saved.put("wikidata", this.serviceUrls.get("wikidata"));
    this.serviceUrls.put("wikidata", setWikidataService.get("wikidata"));
    return saved;
  }
}
