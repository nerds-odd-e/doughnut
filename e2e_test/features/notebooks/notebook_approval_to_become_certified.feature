Feature: Notebook approval to become certified

  As a trainer, I want to request approval for my notebook to be certified.
  As an admin, I want to approve the notebook for giving out certificate,
  so that only approved notebooks can be used for certification.

  Background:
    Given I am logged in as an existing user
    And I have the following notebooks:
      | TDD |
      | GIT |
      | COW |

  Scenario Outline: I only receive a certificate on passing an approved assessment
    Given there is a <Notebook State> "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
    When I pass the assessment on "Just say 'Yes'"
    Then I <Can Or Cannot> download a certificate after passing an assessment

    Examples:
      | Notebook State     | Can Or Cannot |
      | certified notebook | can           |
      | notebook           | cannot        |

  Scenario: Apply for an approval for a notebook
    When I request for an approval for notebooks:
      | TDD |
    Then I should see the status "Pending" of the approval for notebook "TDD"

  Scenario: Notebook with no approval request does not show on the approval request list
    Given I am logged in as an admin
    And I have a notebook with the head note "WrumWrum"
    # When I open certification approval page
    Then I should not see any pending approval requests

  Scenario: Approval list shows pending requests for notebooks
    When I request for an approval for notebooks:
      | TDD |
      | GIT |
    When I am logged in as an admin
    And I open certification approval page
    Then I should see following notebooks waiting for approval:
      | TDD |
      | GIT |

  Scenario: Approved notebook is removed from approval list
    Given following notebooks have pending approval:
      | TDD |
      | GIT |
    And I am logged in as an admin
    When I approve notebook "TDD"
    Then I should not see notebook "TDD" waiting for approval

  Scenario: Reset approval on new question
    Given I am logged in as an admin
    And I have a notebook with the head note "The cow joke"
    And I request for an approval for notebooks:
      | The cow joke |
    When I add the following question for the note "The cow joke":
      | Stem                                     | Choice 0              | Choice 1 | Choice 2 | Correct Choice Index |
      | Why do cows have hooves instead of feet? | Because they lactose! | Woof!    | What?    | 0                    |
    Then I can request approval for the notebook "The cow joke"

  Scenario: Seeing approved notebooks
    Given I am logged in as an admin
    And I have a notebook with the head note "Grape"
    And I choose to share my notebook "Grape"
    And I request for an approval for notebooks:
      | Grape |
    Then I should see following notebooks waiting for approval:
      | Grape |
    And I approve notebook "Grape"
    Then I should see a certification icon on the "Grape" notebook card
