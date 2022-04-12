Feature: Read Comment
  As a learner, I want to read comments on a note.

  Scenario: No comments on a note
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title        |
      | Less is More |
    When I visit note "Less is More"
    Then I should see an input box for comment


#  Comment on Ivan's own note
#  Given Ivan is user
#  And Ivan has a note "to do list"
#  When Ivan comment on the note "to do list" with "add more stuff later"
#  Then Ivan should see the comment "add more stuff later" under the note "to do list"
