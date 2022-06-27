Feature: Initial learning
  As a learner, I want to add note to my future review list.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    |
      | hard     |
      | easy     |

  Scenario: Update review setting
    Given I am changing note "hard"'s review setting
    And I have selected the option "Remember Spelling" in review setting and set the level to be 2
    And I learned one note "easy" on day 1
    When I am learning new note on day 3
    Then I should see the option "Remember Spelling" is "on"
    When I have unselected the option "Remember Spelling"
    And I am changing note "hard"'s review setting
    Then I should see the option "Remember Spelling" is "off"

