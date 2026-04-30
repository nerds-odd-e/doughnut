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
    And I have a notebook "TDD study"
    And I have a notebook "Git guide"
    Given I request for an approval for notebook "TDD study"
    And I request for an approval for notebook "Git guide"
    * I should see the status "Pending" of the approval for notebook "TDD study"
    When I am re-logged in as an admin
    And I approve notebook "TDD study" to become certified
    Then I should see following notebooks waiting for approval only:
      | Git guide |
