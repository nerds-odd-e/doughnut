Feature: Get certificate by an assessment
  As a trainer, I want to provide certificate to the learner when they pass the assessment,
  so that they can use it to show their skill level on the topic.

  As a learner, I want to obtain a certificate when I pass the assessment.

  Background:
    Given I am logged in as an existing user
    And there is an assessment on notebook "Just say 'Yes'" with 2 questions certified by "Korn"

  Scenario: I should receive a certificate when pass the assessment
    When I get score <Score> when do the assessment on "Just say 'Yes'"
    Then I should <Receive or not> a certificate of "Just say 'Yes'" certified by "Korn"

    Examples:
      | Score | Receive or not |
      | 2/2   | receive        |
      | 1/2   | not receive    |

  Scenario: As a learner, I receive Certification with correct expiration date
    Given the certificate expiration period for the notebook "Just say 'Yes'" is <expired days> days
    And today is "<today>"
    When I finish the assessment for the notebook "Just say 'Yes'" with score 2/2
    Then I should receive my "Just say 'Yes'" certificate with the issue date today and expiring on "<expiration date>"

    Examples:
      | today      | expired days | expiration date |
      | 2024-07-15 | 100          | 2024-10-23      |
      | 2024-01-01 | 80           | 2024-03-21      |
