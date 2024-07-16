Feature: Notebook edit

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Topic | Parent Topic   |
      | team  | LeSS in Action |

  Scenario: Edit Certification expire period
        When I set the number of Certificate Expiration Period of the notebook "LeSS in Action" to 100
        Then I should see the expiration period of the notebook "LeSS in Action" to 100


