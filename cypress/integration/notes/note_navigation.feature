Feature: Note navigation
  As a learner, I want to browse my notes.

  Background:
    Given I've logged in as an existing user

  Scenario Outline: Navigation
    Given there are some notes for the current user
      | title                | testingParent |
      | Shape                |               |
      | Rectangle            | Shape         |
      | Square               | Rectangle     |
      | Triangle             | Shape         |
      | Equilateral triangle | Triangle      |
      | Circle               | Shape         |
    When I open "Shape/Triangle" note from top level
    But I should be able to go to the "<button>" note "<expected title>"

    Examples:
      | button           | expected title       |
      | next             | Equilateral triangle |
      | next sibling     | Circle               |
#     | previous         | Square               |
      | previous sibling | Rectangle            |
