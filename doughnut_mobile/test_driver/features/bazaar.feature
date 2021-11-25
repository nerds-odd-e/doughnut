Feature: Bazaar browsing
  Part of the bazaar should be visible to everyone.

  Background:
    Given there is a notebook "Shape" in the bazaar

  Scenario: Browsing as non-user
    When I haven't login
    Then I should see "Shape" is shared in the Bazaar
