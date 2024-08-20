@wip
@skip
Feature: Editing notebook certificate expiration

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the name "Certified thing"

  Scenario: Edit the default expiration timespan for a certificate
    When I open the notebooks settings
    When I change the expiration timespan to 2 years
    Then I should see the expiration timespan which is 2 years

