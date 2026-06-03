Feature: Creating circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Scenario: Invite by invitation code
    Given circle "Odd-e Invite Circle" exists for "old_learner" with invitation link saved
    And I am re-logged in as "another_old_learner"
    When I visit the invitation link
    And I join the circle
    Then I should see the circle "Odd-e Invite Circle" and it has two members in it

  Scenario: New user via circle invitation
    Given circle "Odd-e New User Circle" exists for "old_learner" with invitation link saved
    And my session is logged out
    When I visit the invitation link
    And I identify myself as a new user
    Then I should be asked to create my profile
    When I save my profile with:
      | Name      |
      | Learner A |
    And I am re-logged in as "user"
    And I join the circle
    Then I should see the circle "Odd-e New User Circle" and it has two members in it
