Feature: Certification expiration
  As a trainer, I want to set the expiration date of the certificate of the assessment,
  so that the learner knows when the certificate expires.

  Background:
    Given the current date is "2024-01-01"
    And I am logged in as "a_trainer"
    And there is a certified notebook "Certified thing" by "a_trainer" with 2 questions and is shared to the Bazaar

  Scenario: User gets certificate with expiration date
    Given I set the certificate expiry of the notebook "Certified thing" to "2y"
    When I complete an assessment for the notebook "Certified thing"
    Then I should see that the certificate of "Certified thing" assesment expires on "2026-01-01"

  Scenario: Updating the expiry should note change the existing certificates
    Given I set the certificate expiry of the notebook "Certified thing" to "2y"
    And I complete an assessment for the notebook "Certified thing"
    When I set the certificate expiry of the notebook "Certified thing" to "3y"
    Then list should contain certificates
      |Notebook         |Expiry Date  |
      |Certified thing  |2026-01-01   |


