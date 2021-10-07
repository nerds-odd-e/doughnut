Feature: feature toggle

  @ignore @featureToggle
  Scenario: A scenario with feature toggle should toggle the feature
    Then The "Feature Toggle is On" alert "should exist"

  Scenario: A scenario without feature toggle should toggle the feature off
    Then The "Feature Toggle is On" alert "should not exist"
