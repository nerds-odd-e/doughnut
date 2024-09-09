Feature: Notebook approval to become certified

  As a trainer, I want to request approval for my notebook to be certified.
  As an admin, I want to approve the notebook for giving out certificate,
  so that only approved notebooks can be used for certification.

  Scenario Outline: User only receive a certificate on passing an approved assessment
    Given there is a <Notebook State> "Special training" by "a_trainer" with 2 questions, shared to the Bazaar
    * I should see <Certificate Indicator> on the "Special training" notebook card in the bazaar
    And I am logged in as an existing user
    When I pass the assessment on "Special training"
    Then I <Can Or Cannot> download a certificate after passing an assessment

    Examples:
      | Notebook State     | Certificate Indicator | Can Or Cannot |
      | certified notebook | a certification icon  | can           |
      | notebook           | no certification icon | cannot        |

  Scenario: Apply for an approval for a notebook
    Given I am re-logged in as 'a_trainer'
    And I have a notebook with the head note "TDD"
    And I have a notebook with the head note "GIT"
    Given I request for an approval for notebook "TDD"
    And I request for an approval for notebook "GIT"
    * I should see the status "Pending" of the approval for notebook "TDD"
    When I am re-logged in as an admin
    And I approve notebook "TDD" to become certified
    Then I should see following notebooks waiting for approval only:
      | GIT |

  Scenario: Reset approval on new question
    Given I am re-logged in as an admin
    And I have a notebook with the head note "The cow joke"
    And I request for an approval for notebook "The cow joke"
    And I approve notebook "The cow joke" to become certified
    When I add the following question for the note "The cow joke":
      | Stem                                     | Choice 0              | Choice 1 | Choice 2 | Correct Choice Index |
      | Why do cows have hooves instead of feet? | Because they lactose! | Woof!    | What?    | 0                    |
    Then I can request approval for the notebook "The cow joke" again
