Feature: Notebook certificate expiration 

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the name "Certified thing"

  Scenario: See the default expiration timespan for a certificate
    Then I should see the default expiration of "Certified thing" note to be 1 year
