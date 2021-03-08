Feature: Creating circles
  As a learner, I want to create circles so that I can own notes together with other people.

  @ignore
  Scenario: Invite by invitation code
    Given I've logged in as an existing user
    And I create a new circle "Odd-e SG Team" and copy the invitation code
    When I've logged in as another existing user
    And I join the circle with the invitation code
    Then I should see the circle "Odd-e SG Team" and it has two members in it
