Feature: Notebook approval

  Background:
    Given I am logged in as an existing user
    And I have the following notebooks:
      | TDD           |
      | GIT           |
      | COW           |


  Scenario: Apply for an approval for a notebook
    When I request for an approval for notebooks:
      | TDD           |
    Then I should see the status "Pending" of the approval for notebook "TDD"

  Scenario: Approval cannot be requested again after requesting it
    When I request for an approval for notebooks:
      | TDD           |
    Then I cannot request approval again for notebook "TDD"
  
  Scenario: Notebook with no approval request does not show on the approval request list
      Given I am logged in as an admin
      And I have a notebook with the head note "WrumWrum"
      # When I open certification approval page
      Then I should not see any pending approval requests

  Scenario: Approval list shows pending requests for notebooks
    When I request for an approval for notebooks:
    | TDD           |
    | GIT           |
    When I am logged in as an admin
    And I open certification approval page
    Then I should see following notebooks waiting for approval:
    | Notebook name | Username            | Approve |
    | TDD           | old_learner         | Approve |
    | GIT           | another_old_learner | Approve |

  Scenario: Approved notebook is removed from approval list
      Given following notebooks have pending approval:
      | TDD           |
      | GIT           |
      And I am logged in as an admin
      When I approve notebook "TDD"
      Then I should not see notebook "TDD" waiting for approval

