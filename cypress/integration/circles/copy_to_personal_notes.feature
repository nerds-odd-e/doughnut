Feature: Copy a circle notebook to personal notes
  As a user I want to create a copy of a notebook from the circle into my personal notes.

  Background:
    Given I've logged in as an existing user
    And I navigate to an existing circle "Odd-e SG Team" I belong to
    And a notebook already exists in the circle

  @ignore
  Scenario: User 1 wants to have his own notes
    Given Two users sharing a notebook
    When one user makes a copy of the notebook
    Then the user has a copy of the notebook in his private notes

  @ignore
  Scenario: New user via circle invitation
    Given Two users sharing a notebook
    And one user having made a first copy of it into its private notes
    When one user makes a second copy of the notebook
    Then that user has two different copies of the notebook in his private notes

