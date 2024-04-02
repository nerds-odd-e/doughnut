Feature: Extract Child Note

  @ignore
  Scenario: Case 1
    Given I have a note detail
    When I select a text
    And I extract note
    Then New child note detail will be added

  @ignore
  Scenario: Case 2
    Given I have a note detail
    When I not select any text
    Then Extract button will be disabled
