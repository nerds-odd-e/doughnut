@wip
@skip
Feature: Notebook certificate default expiration

  Background:
    Given I am logged in as an existing user
    When There is a "Certified thing" notebook with assesment that has certification

  Scenario: See the default expiration +1 year from now for a certificate
    Given Now is "2024-01-01"
    When I Complete an assessment in "Certified thing"
    Then I should see that the certificate of "Certified thing" assesment expires on "2025-01-01"
