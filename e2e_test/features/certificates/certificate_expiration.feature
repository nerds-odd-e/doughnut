Feature: Certification expiration
  As a trainer, I want to set the expiration date of the certificate of the assessment,
  so that the learner knows when the certificate expires.

  Background:
    Given the current date is "2024-01-01"
    And I am logged in as "a_trainer"
    And there is a certified notebook "Certified thing" by "a_trainer" with 2 questions, shared to the Bazaar

  Scenario: Certificate is issued with the specified expiration date
    Given I set the certificate expiry of the notebook "Certified thing" to "5y"
    When I complete an assessment for the notebook "Certified thing"
    Then I should have the following certificates:
      | Notebook        | Expiry Date  |
      | Certified thing | 2029-01-01   |

  Scenario: Changing certificate expiry does not affect existing certificates
    Given I set the certificate expiry of the notebook "Certified thing" to "2y"
    And I complete an assessment for the notebook "Certified thing"
    When I set the certificate expiry of the notebook "Certified thing" to "3y"
    Then I should have the following certificates:
      | Notebook        | Expiry Date  |
      | Certified thing | 2026-01-01   |

  Scenario: a certified notebook should remain certified after adding a new question
    Given I add the following question for the note "Certified thing":
      | Stem                                 | Choice 0    | Choice 1 | Choice 2 | Correct Choice Index |
      | What do you call a cow with not leg? | Ground beef | Cowboy   | Oxford   | 0                    |
    Then I should see a certification icon on the "Certified thing" notebook card in the bazaar
