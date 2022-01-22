Feature: Copy a circle notebook to personal notes
  As a user I want to create a copy of a notebook from the circle into my private notes,
  so that I can make my own changes without affecting the shared notebook.

  Background:
    Given I've logged in as the existing user "old_learner"

  @featureToggle
  Scenario: User 1 wants to have his own notes
    When I make a copy of the existing notebook "Test notebook"
    Then I have a copy of the notebook "Test notebook" in the private notes
