Feature: feature toggle
  As a developer or Product Owner, I want to toggle some feature off in production by default,
  so that the end-user won't see the unfinished feature.

  @featureToggle
  Scenario: A scenario with feature toggle should toggle the feature
    Then The "Feature Toggle is On" alert "should exist"

  Scenario: A scenario without feature toggle should toggle the feature off
    Then The "Feature Toggle is On" alert "should not exist"
    When I go to the testability page to turn on the feature toggle
    Then The "Feature Toggle is On" alert "should exist"
