@usingMockedOpenAiService
Feature: Delete Understanding Check Points
  As a learner, when I review the understanding checklist,
  I want to delete selected points and have AI remove the related content from note details,
  So that the note becomes more focused on the remaining key points.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Sample" and details "First point. Second point. Third point. Fourth point. Fifth point."

  Scenario: Successfully delete selected understanding points
    Given OpenAI generates understanding checklist with points:
      | First point  |
      | Second point |
      | Third point  |
      | Fourth point |
      | Fifth point  |
    And OpenAI returns the following details when requested to delete points:
      | Second point. Fourth point. Fifth point. |
    When I am assimilating the note "Sample"
    Then I should see an understanding checklist with a maximum of 5 points
    When I delete understanding points 0 and 2
    Then the note details on the current page should be "Second point. Fourth point. Fifth point."
