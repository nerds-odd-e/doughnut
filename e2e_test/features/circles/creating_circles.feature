Feature: Creating circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Background:
    Given I've logged in as an existing user
    And I create a new circle "Odd-e SG Team" and copy the invitation code

  Scenario: Invite by invitation code
    Given I've logged in as another existing user
    When I visit the invitation link
    And I join the circle
    Then I should see the circle "Odd-e SG Team" and it has two members in it

  Scenario: New user via circle invitation
    Given my session is logged out
    When I visit the invitation link
    And I identify myself as a new user
    Then I should be asked to create my profile
    When I save my profile with:
      | Name      |
      | Learner A |
    And I join the circle
    Then I should see the circle "Odd-e SG Team" and it has two members in it
