@usingMockedGithubService
Feature: Export Notebook to GitHub
  As a learner,
  I want to export my notebooks directly to a GitHub repository
  So that I can version control my notes and share them with others through GitHub

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Programming" and notes:
      | Title          | Details                    | Parent Title |
      | Python         | A programming language     | Programming  |@
      | Data Types    | Basic Python data types    | Python       |
      | Functions     | How to define functions    | Python       |
  
  Scenario: Successfully export notebook to GitHub
    When I go to Notebook page
    And I export notebook "Programming" to GitHub
    And I input repository name "study-notes"
    Then I should see a success message "Notebook exported to GitHub successfully" 