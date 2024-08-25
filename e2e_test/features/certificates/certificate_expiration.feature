Feature: Certification expiration

  Background:
    Given Now is "2024-01-01"
    And I am logged in as an existing user
    And there is a notebook "Certified thing" by "old_learner" with approved certifiable assessment

  Scenario: Default certificate expiration is one year
    When I Complete an assessment in "Certified thing"
    Then I should see that the certificate of "Certified thing" assesment expires on "2025-01-01"

  Scenario: See modifed expiration date
    And Expiration of "Certified thing" is set to "2y"
    * I should see the expiration setting of "Certified thing" is set to "2y"
    When I Complete an assessment in "Certified thing"
    Then I should see that the certificate of "Certified thing" assesment expires on "2026-01-01"

  Scenario: Existing certificate expiry is not changed
    And Expiration of "Certified thing" is set to "2y"
    And I Complete an assessment in "Certified thing"
    When Expiration of "Certified thing" is set to "3y"
    Then list should contain certificates
      |Notebook         |Expiry Date  |
      |Certified thing  |2026-01-01   |


