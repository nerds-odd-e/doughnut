Feature: Note parent reference
  As a learner, I want to create child note that can refer to its parent,
  so that I can indicate the relationship between the notes.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Title                    | Parent Title   |
      | Usually, %P is in-person | LeSS in Action |

  Scenario: the note should be rendered with parent reference
    When I navigate to "My Notes/LeSS in Action" note
    Then I should see a child note "Usually, [LeSS in Action] is in-person"

  Scenario: the topic constructor is editable
    When I navigate to "My Notes/LeSS in Action/Usually, [LeSS in Action] is in-person" note
