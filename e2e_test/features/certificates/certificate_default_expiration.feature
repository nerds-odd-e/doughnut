@wip
@skip
Feature: Notebook certificate expiration 

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the name "Certified thing"

  Scenario: See the default expiration timespan for a certificate
    When I open the notebooks settings
    Then I should see the default expiration timespan which is 1 year
