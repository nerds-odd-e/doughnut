@wip
@skip
Feature: Notebook certificate expiration

  Background:
    Given I am logged in as an existing user
    And There exists a notebook with the name "Certified thing"
    And The notebook has an assessment with certification

  Scenario: See the expiration +2 years from now for a certificate
    Given I open the notebooks settings
    And I change the expiration timespan to 2 years
    When I Complete an assessment
    Then I should see that the certificate expires in 2 years from now
