Feature: Feature toggle
  As a developer or Product Owner, I want unfinished features hidden by default in production,
  so that end users do not see them until the toggle is turned on.

  @featureToggle
  Scenario: Feature toggle on shows the unfinished feature indicator
    When I visit the home page
    Then I should see the unfinished feature indicator

  Scenario: Feature toggle off hides the indicator until turned on
    When I visit the home page
    Then I should not see the unfinished feature indicator
    When I turn on the feature toggle
    Then I should see the unfinished feature indicator
