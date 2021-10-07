Feature: feature toggle

  @ignore @featureToggle
  Scenario: A scenario with feature toggle should toggle the feature
    Then I should see the "Feature Toggle is On" alert in the page
