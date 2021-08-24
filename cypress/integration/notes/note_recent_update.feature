@ignore
Feature: note update
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on only reviewing the newly added notes.

  Background:
    Given there are some notes for existing user "old_learner"
      | title                | testingParent |
      | Shape                |               |
      | Rectangle            | Shape         |
      | Square               | Rectangle     |
      | Triangle             | Shape         |
      | Equilateral triangle | Triangle      |
      | Circle               | Shape         |

  Scenario: View an indicator when I add a new note
    Given I've logged in as an existing user
    When I create note belonging to "Equilateral triangle":
      | Title | Description |
      | Small Triangle | This is a small equilateral triangle |
    Then I should see new note banner
    When I open "Shape" note from the top level
    And I edit "Shape" note description to become "new description"
    Then I should see new note banner
    When I open "Shape" note from the top level
    Then I should see these notes as children marked as new with a pink border
      | note-title |
      | Triangle |
      | Circle   |
    When I open "Shape/Triangle" note from top level
    Then I should see these notes as children marked as new with a pink border
      | note-title |
      | Equilateral triangle |
    When I open "Shape/Triangle/Equilateral triangle" note from top level
    Then I should see these notes as children marked as new with a pink border
      | note-title |
      | Small Triangle |


  Scenario: View an indicator for new notes when I view other people's notes
    Given I've logged in as another existing user
    And I have access to "old_learner" notebook "Shape"
    When I open "Shape" note from the top level
    Then I should see these notes as children marked as new with a pink border
      | note-title |
      | Triangle |
      | Circle   |
    When I open "Shape/Triangle" note from top level
    Then I should see these notes as children marked as new with a pink border
      | note-title |
      | Equilateral triangle |
    When I open "Shape/Triangle/Equilateral triangle" note from top level
    Then I should see these notes as children marked as new with a pink border
      | note-title |
      | Small Triangle |
    When I open "Shape/Triangle/Equilateral triangle/Small Triangle" note from top level
    Then I should see new note banner


  Scenario: When 12 hours have lapsed after note was updated it should not be marked as new
    Given I've logged in as an existing user 12 hours later
    When I open "Shape" note from the top level
    Then I should not see these notes as children marked as new with a pink border
      | note-title |
      | Triangle |
      | Circle   |
    When I open "Shape/Triangle" note from top level
    Then I should not see these notes as children marked as new with a pink border
      | note-title |
      | Equilateral triangle |
    When I open "Shape/Triangle/Equilateral triangle" note from top level
    Then I should not see these notes as children marked as new with a pink border
      | note-title |
      | Small Triangle |