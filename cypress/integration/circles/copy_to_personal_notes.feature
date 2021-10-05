Feature: Copy a circle mindmap to personal notes
  As a user I want to create a copy of a mindmap from the circle into my personal notes.

  Background:
    Given I've logged in as an existing user
    And I navigate to an existing circle "Odd-e SG Team" I belong to
    And a mindmap already exists in the circle

  @ignore
  Scenario: User 1 wants to have his own notes
    Given Two users sharing a mindmap
    When one user makes a copy of the mindmap
    Then the user has a copy of the mindmap in his private notes

  @ignore
  Scenario: New user via circle invitation
    Given Two users sharing a mindmap
    And one user having made a first copy of it into its private notes
    When one user makes a second copy of the mindmap
    Then that user has two different copies of the mindmap in his private notes

