package com.odde.doughnut.testability;

import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.GithubService;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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

  @Getter private String wikidataServiceUrl = "https://www.wikidata.org";

  public void timeTravelTo(Timestamp timestamp) {
    this.timestamp = timestamp;
    if (nonRandomizer == null) {
      nonRandomizer = new NonRandomizer();
    }
  }

  // To return a Timestamp as a Bean,
  // we need to make it not final, so that CGLIB can create a subclass
  static class MyTimestamp extends Timestamp {
    public MyTimestamp(long time) {
      super(time);
    }
  }

  @Bean("currentUTCTimestamp")
  @SessionScope
  public MyTimestamp getCurrentUTCTimestamp() {
    if (timestamp == null) {
      return new MyTimestamp(System.currentTimeMillis());
    }
    return new MyTimestamp(timestamp.getTime());
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

  public String setWikidataService(String wikidataServiceUrl) {
    String saved = this.wikidataServiceUrl;
    this.wikidataServiceUrl = wikidataServiceUrl;
    return saved;
  }
}
