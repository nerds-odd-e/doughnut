Feature: Get Certificate by an assessment.
  As a learner, I want to get the certificate when I do assessment passed,
  so that I can show my certificate on my portfolio.

  Scenario: As a learner, I receive a certificate when pass the assessment.
    Given I am logged in as an existing user
    And I have shared assessment with 2 questions in nootbook "Countries" with certified by "<certified by>"
    When I get <score> percent score when do the assessment on "Countries"
    Then I should "<receive or not>" my certificate of "Countries" certified by "<certified by>"

    Examples:
      | score | receive or not | certified by |
      | 100   | receive        | Korn         |
      | 50    | not receive    | Mindo        |

  @ignore
  Scenario: As a learner, I receive Certification with correct expiration date
    Given I am logged in as an existing user
    And   The note owner sets the certificate expiration period for the "Countries" notebook to 100 days
    When  I pass the assessment for the "Countries" notebook today
    Then  I should receive my "Countries" certificate with the issue date today and expiring in 100 days later
