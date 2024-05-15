Feature: Note parent reference
  As a learner, I want to create child note that can refer to its parent,
  so that I can indicate the relationship between the notes.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor         | testingParent  |
      | LeSS in Action           |                |
      | Usually, %P is in-person | LeSS in Action |

  Scenario: the note should be rendered with parent reference
    When I navigate to to "My Notes/LeSS in Action" note
    Then I should see a child note "Usually, [LeSS in Action] is in-person"

  Scenario: the topic constructor is editable
    When I navigate to to "My Notes/LeSS in Action/Usually, [LeSS in Action] is in-person" note
