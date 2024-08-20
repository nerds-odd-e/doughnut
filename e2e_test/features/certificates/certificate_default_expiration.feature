@wip
@skip
Feature: Notebook certificate default expiration

  Background:
    Given I am logged in as an existing user
    And There exists a notebook with the name "Certified thing"
    And The notebook has an assessment with certification

  Scenario: See the default expiration +1 year from now for a certificate
    When I Complete an assessment
    Then I should see that the certificate expires in 1 year from now
