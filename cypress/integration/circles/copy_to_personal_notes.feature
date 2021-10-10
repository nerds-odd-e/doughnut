Feature: Copy a circle notebook to personal notes
  As a user I want to create a copy of a notebook from the circle into my private notes,
  so that I can make my own changes without affecting the shared notebook.

  Background:
    Given I've logged in as the existing user "old_learner"
    And I navigate to an existing circle "Odd-e SG Team" where the "old_learner" and "another_old_learner" users belong to
    And the notebook "Test notebook" already exists in the circle "Odd-e SG Team"

  @featureToggle
  Scenario: User 1 wants to have his own notes
    When I make a copy of the existing notebook "Test notebook"
    Then I have a copy of the notebook "Test notebook" in the private notes
