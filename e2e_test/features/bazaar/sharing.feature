Feature: Bazaar sharing
  There should be a bazaar in Doughnut that people can share their public notes,
  and others can subscribe to their notes.

  Background:
    Given I am logged in as an existing user

  Scenario: Contributing To Bazaar
    Given I have a notebook "Geometry set" with a note "Shape" and notes:
      | Title    | Details                   |
      | Square   | four equal straight sides |
      | Triangle | three sides shape         |
    When I choose to share my notebook "Geometry set"
    Then I should see "Geometry set" shared in the Bazaar

