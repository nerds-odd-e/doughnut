Feature: Note display
  As a learner, I want to read my note or show my note
  to other people.

  Background:
    Given I've logged in as an existing user

  Scenario: Long description is abbreviated in card view
    Given there are some notes for the current user
      | title                                   | description                                                                                                                                                                                                 |
      | Potentially shippable product increment | The output of every Sprint is called a Potentially Shippable Product Increment. The work of all the teams must be integrated before the end of every Sprint—the integration must be done during the Sprint. |
    Then I should see these notes belonging to the user at the top level of all my notes
      | note-title                              | note-description                                   |
      | Potentially shippable product increment | The output of every Sprint is called a Potentia... |

  Scenario: Article view
    Given there are some notes for the current user
      | title                | description | testingParent |
      | Shape                | content     |               |
      | Rectangle            | content     | Shape         |
      | Square               | content     | Rectangle     |
      | Triangle             | content     | Shape         |
      | Equilateral triangle | content     | Triangle      |
      | Circle               | content     | Shape         |
    When I open the note "Shape" in my notes in article view
    Then I should see in the article:
      | level | title    |
      | h1    | Shape    |
      | h2    | Triangle |
    When I click "Triangle" in article view
    Then I should see in the article:
      | level | title                |
      | h1    | Triangle             |
      | h2    | Equilateral triangle |

