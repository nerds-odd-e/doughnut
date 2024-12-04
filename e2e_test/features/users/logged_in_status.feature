Feature: Logged in status

  Background:
    Given I am logged in as an existing user


  Scenario: logout
    When I logout via the UI
    Then I should be on the welcome page and asked to login

  Scenario: User logout because of session timeout
    Given I have a notebook with head note "Shape" and notes:
      | Title    | Parent Title |
      | Triangle | Shape        |
    And I navigate to "My Notes/Shape" note
    When my session is logged out
    Then I should be asked to log in again when I click the link "Triangle"

  @mockBrowserTime
  @usingMockedOpenAiService
  Scenario: Session timeout out when post
    Given I added and learned one note "Fungible" on day 1
    And I am recalling my note on day 2
    And my session is logged out
    When I choose yes I remember
    Then I login as "old_learner" I should see "Fungible"
