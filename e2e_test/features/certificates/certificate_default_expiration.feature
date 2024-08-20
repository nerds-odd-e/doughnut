@wip
Feature: Notebook certificate default expiration

  Background:
    Given I am logged in as an existing user
    When There is a "Certified thing" notebook with assesment that has certification

  Scenario: See the default expiration +1 year from now for a certificate
    When I Complete an assessment in "Certified thing"
    Then I should see that the certificate of "Certified thing" assesment expires in 1 year from now
