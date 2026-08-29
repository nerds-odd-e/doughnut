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
    When I continue from the Daily probe
    Then I should see ordinary recall

  Scenario: Learner with Daily probe off enters recall unchanged
    When I visit recall
    Then I should not see the Daily probe instruction
    And I should see ordinary recall
