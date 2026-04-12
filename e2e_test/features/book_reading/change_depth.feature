Feature: Change depth of book blocks

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    When I attach a book with MinerU fixture "refactoring" to the notebook "Top Maths"
    And I open the book attached to notebook "Top Maths"

  Scenario: Focus a book block in the layout
    When I choose the book block "2. The Usual Defi nition Is Not Enough"
    Then the book block "2. The Usual Defi nition Is Not Enough" should be focused in the book layout
