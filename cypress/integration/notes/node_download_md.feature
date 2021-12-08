Feature: Download Note to .md file
    As a user, I want to be able to download note that currently open as .md file.


  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title            | testingParent | description |
      | Singapore        |               |             |
      | History          | Singapore     |             |
      | Geography        | Singapore     |             |
      | Leaving Malaysia | History       | in 1965     |
    And I open the "article" view of note "Singapore"

  @featureToggle
  Scenario: User download current note
    Then I download note
    Then There is a "Singapore.md" file downloaded