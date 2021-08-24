@ignore
Feature: note update
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on only reviewing the newly added notes.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title                | testingParent |
      | Shape                |               |
      | Rectangle            | Shape         |
      | Square               | Rectangle     |
      | Triangle             | Shape         |
      | Equilateral triangle | Triangle      |
      | Circle               | Shape         |

  Scenario: View an indicator when I add a new note
    When I create note belonging to "Equilateral triangle":
      | Title | Description |
      | Small Triangle | This is a small equilateral triangle |
    Then I should see new note banner
    When I open "Shape" note from the top level
    Then I should see these notes as children marked as new
      | note-title |
      | Triangle |
    When I open "Shape/Triangle" note from top level
    Then I should see these notes as children marked as new
      | note-title |
      | Equilateral triangle |
    When I open "Shape/Triangle/Equilateral triangle" note from top level
    Then I should see these notes as children marked as new
      | note-title |
      | Small Triangle |