Feature: Repetition
  As a learner, I want to be tested in repetition and also update my memory status.

  Background:
    Given I've logged in as an existing user

  Scenario: I can remove a note from further reviews
    Given I added and learned one note "Fungible" on day 1
    And I am repeat-reviewing my old note on day 2
    When I choose the happy option
    And I view the last result
    When choose to remove it from reviews
    Then I should see the review point is removed from review
    And On day 100 I should have "0/0" note for initial review and "0/0" for repeat
