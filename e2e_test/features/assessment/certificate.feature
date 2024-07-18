Feature: Get Certificate by an assessment.
  As a learner, I want to get the certificate when I do assessment passed,
  so that I can show my certificate on my portfolio.

  @ignore
  Scenario: As a learner, I receive a certificate when pass the assessment.
    Given I am logged in as an existing user
    Given I have 2 questions in nootbook "Countries" and 2 assessments on notebook
    When I get <score> percent score when do the assessment on "Countries"
    Then I should "<receive or not>" my certificate of "Countries" certified by "<certified by>"

    Examples:
      | score | receive or not | certified by |
      |   100 | receive        | Korn         |
      |    80 | receive        | Jens         |
      |    20 | not receive    | Mindo        |
