Feature: Read Comment
  As a learner, I want to read comments on a note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title        |
      | Less is More |
  
  @featureToggle
  Scenario: No comments on a note
    When I visit note "Less is More"
    Then I should see an input box for comment

  @ignore
  Scenario: Add a comment on a note
    When I visit note "Less is More"
    And I add a comment "hello world"
    Then I should see "hello world" in the page

#  Comment on Ivan's own note
#  Given Ivan is user
#  And Ivan has a note "to do list"
#  When Ivan comment on the note "to do list" with "add more stuff later"
#  Then Ivan should see the comment "add more stuff later" under the note "to do list"
