Feature: Add a new comment to a note
  As a user, I want to add a new comment to an existing note


  @featureToggle
  Scenario: Add a new comment to my note
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | testingParent | description        |
      | My note |               | My individual note |

    When I open "My note" note from top level
    And I click the add comment button
    Then I should be able to add a comment with description "please elaborate"

  @ignore
  Scenario: Add a new comment to another person's note
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    And Someone seed a notebook "My circle - Old learner's Notebook" in circle "Odd-e SG Team"
    And there are some notes for existing user 'another_old_learner'
      | title                         | testingParent | description         |
      | My circle - Another user note |               | An awesome training |

    When I select a note with title 'My circle - Another user note'
    And I click the add comment button
    Then I should see a new comment input box displayed
    And I input 'A comment to another user\'s note (same circle)' in the comment input box
    And I click outside the input box
    Then I should see comment added to note