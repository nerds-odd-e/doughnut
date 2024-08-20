@skip
Feature: Notebook approval

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Sedation"

  Scenario: Apply for an approval for a notebook
    When I apply for an approval for notebook "Sedation"
    Then I should see the status "Pending" of the approval for notebook "Sedation"


Scenario: Empty approval list is shown
    Given there are no pending approvals
    When I open certification approval page
    Then I should see empty approval list

Scenario: Approval list shows pending requests for notebooks
    Given that I have the following notebooks:
    | TDD           | 
    | GIT           |
    | COW           |
    And I request for approval for notebooks:
    | TDD           | 
    | GIT           |
    When I open certification approval page
    Then I should see following notebooks waiting for approval:
    | Notebook name | Username            | Approve |
    | TDD           | old_learner         | Approve |
    | GIT           | another_old_learner | Approve |

Scenario: Approved notebook is removed from approval list
    Given that I have the following notebooks:
    | Notebook name |
    | TDD           | 
    | GIT           |
    | COW           |
    And I request for approval for notebooks:
    | TDD           | 
    | GIT           |
    When approve notebook "GIT"
    Then I should see following notebooks waiting for approval:
    | Notebook name | Username            | Approve |
    | TDD           | old_learner         | Approve |

