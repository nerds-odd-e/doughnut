Feature: Add, Reply or Delete a comment
  As a learner, I can add a comment or reply to comment/s
  on a note I own or in a circle I belong to. I can also delete
  a comment I created for the same note.

  Background:
    Given I've logged in as an existing user

  @ignore
  @featureToggle
  Scenario: Add a comment on a note
    And there are some notes for the current user
      | title        |
      | Less is More |
    When I visit note "Less is More"
    And I add a comment "hello world"
    Then I should see "hello world" in the page
    And I should see "Old Learner" in the page
    And I should see comment posted time


  @ignore
  @featureToggle
  Scenario: reply a comment
    And there is a note and some comments of current user
      | comment |
      | hello   |
      | hello2  |
    And I visit note 'A'
    When I reply to comment 'hello' with 'world'
    Then I should see comments
      | comments |
      | hello    |
      | hello2   |
      | world    |

  @ignore
  @featureToggle
  Scenario: delete comment
    And there is a note and some comments of current user
      | content     |
      | hello world |
      | world       |
    When I delete comment "hello world" under Note 'A'
    Then Note 'A' only have one comment 'world'
