Feature: Get Certificate by an assessment.
  As a learner, I want to get the certificate when I do assessment passed,
  so that I can show my certificate on my portfolio.

  Background: Background name
    Given I am logged in as an existing user

  Scenario: As a learner, I receive a certificate when pass the assessment.
    Given I have shared assessment with 2 questions in nootbook "Countries" with certified by "<certified by>"
    When I get <score> percent score when do the assessment on "Countries"
    Then I should <receive or not> my certificate of "Countries" certified by "<certified by>"

    Examples:
      | score | receive or not | certified by |
      | 100   | receive        | Korn         |
      | 50    | not receive    | Mindo        |

  Scenario: As a learner, I receive Certification with correct expiration date
    Given I have shared assessment with 2 questions in nootbook "Countries" with certified by "Korn"
    And The note owner sets the certificate expiration period for the "Countries" notebook to <expired days> days
    And today is "<today>"
    When I pass the assessment for the "Countries" notebook with score 80
    Then I should receive my "Countries" certificate with the issue date today and expiring on "<expiration date>"

    Examples:
      | today      | expired days | expiration date |
      | 2024-07-15 | 100          | 2024-10-23      |
      | 2024-01-01 | 80           | 2024-03-21      |
