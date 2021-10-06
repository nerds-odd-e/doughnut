Feature: Copy a circle notebook to personal notes
  As a user I want to create a copy of a notebook from the circle into my personal notes.

  Background:
    Given I've logged in as the existing user "old_learner"
    And I navigate to an existing circle "Odd-e SG Team" where the "old_learner" and "another_old_learner" users belong to
    And the notebook "Test notebook" already exists in the circle

  @ignore
  Scenario: User 1 wants to have his own notes
    Given Two users sharing a notebook
    When the user "old_learner" makes a copy of the existing notebook "Test notebook"
    Then the user has a copy of the notebook in his private notes

  @ignore
  Scenario: New user via circle invitation
    Given Two users sharing a notebook
    And one user having made a first copy of it into its private notes
    When one user makes a second copy of the notebook
    Then that user has two different copies of the notebook in his private notes

