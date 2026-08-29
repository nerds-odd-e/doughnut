@disableOpenAiService
@mockBrowserTime
Feature: Daily probe
  As a learner who has opted in
  I want to complete the Daily probe before recall
  So that I can see this run's speed and accuracy and then continue into ordinary recall

  Background:
    Given I am logged in as an existing user
    And there are notes from Note 1 to Note 3
    And the browser and backend are on day 1
    And I assimilate the note "Note 1"

  Scenario: Opted-in learner completes the probe and continues into recall
    Given Daily probe is on
    When I visit recall
    Then I should see the Daily probe instruction
    When I complete the Daily probe
    Then I should see Daily probe speed "4.00"
    And I should see Daily probe accuracy "100%"
    And I should see Daily probe lapses "0"
    And I should see Daily probe variability "0.00"
    And I should see Daily probe saved
    When I continue from the Daily probe
    Then I should see ordinary recall

  Scenario: Leaving recall mid-probe does not save a run
    Given Daily probe is on
    When I visit recall
    Then I should see the Daily probe instruction
    When I visit note "Note 1"
    And it is 2 minutes later in the browser
    And I return to recalling
    Then I should see the Daily probe instruction

  Scenario: A second recall session on the same day skips the probe
    Given Daily probe is on
    When I visit recall
    Then I should see the Daily probe instruction
    When I complete the Daily probe
    And I continue from the Daily probe
    When I visit recall
    Then I should not see the Daily probe instruction
    And I should see ordinary recall

  Scenario: The next local day offers the same Daily probe
    Given Daily probe is on
    When I visit recall
    Then I should see the Daily probe instruction
    When I complete the Daily probe
    And I continue from the Daily probe
    And the browser and backend are on day 2
    When I visit recall
    Then I should see the Daily probe instruction

  Scenario: Learner with Daily probe off enters recall unchanged
    When I visit recall
    Then I should not see the Daily probe instruction
    And I should see ordinary recall

  Scenario: Recall Stats shows the Daily probe trend
    Given Daily probe is on
    When I visit recall
    Then I should see the Daily probe instruction
    When I complete the Daily probe
    And I visit my recall stats
    Then I should see the Daily probe trend

  Scenario: The existing window control filters the Daily probe trend
    Given Daily probe is on
    When I visit recall
    Then I should see the Daily probe instruction
    When I complete the Daily probe
    And I continue from the Daily probe
    And the browser and backend are on day 40
    When I visit recall
    Then I should see the Daily probe instruction
    When I complete the Daily probe
    And I visit my recall stats
    Then I should see 2 days on the Daily probe speed trend
    When I view the last 30 days of trends
    Then I should see 1 day on the Daily probe speed trend
