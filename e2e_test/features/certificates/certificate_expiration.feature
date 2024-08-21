@wip
@skip
Feature: Certification expiration

  Background:
    Given Now is "2024-01-01"
    And I am logged in as an existing user
    And There is a "Certified thing" notebook with assesment that has certification

  Scenario: See modifed expiration date
    Given Expiration of "Certified thing" is set to "2y"
    * I should see the expiration of "Certified thing" is set to "2y"
    When I Complete an assessment in "Certified thing"
    Then I should see that the certificate of "Certified thing" assesment expires on "2026-01-01"

  Scenario: Default certificate expiration is one year
    When I Complete an assessment in "Certified thing"
    Then I should see that the certificate of "Certified thing" assesment expires on "2025-01-01"

  @wip
  @skip
  Scenario: Existing certificate expiry is changed
    Given Expiration of "Certified thing" is set to "2y"
    And I Complete an assessment in "Certified thing"
    When Expiration of "Certified thing" is set to "3y"
    Then I should see that the existing certificate of "Certified thing" assesment expires on "2026-01-01"


