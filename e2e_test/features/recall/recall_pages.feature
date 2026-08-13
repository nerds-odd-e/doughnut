Feature: Assimilation and Recall Pages
  As a learner, I want to assimilate and recall my notes and relationships so that I have fresh memory.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content         | Image Url   |
      | English  |                 |             |
      | Sedition | Incite violence |             |
      | Sedation | Put to sleep    |             |
      | Sedative | Sleep medicine  | a_slide.jpg |
    And the notes "English" are skip-recalled
    And there is "similar to" relationship between note "Sedition" and "Sedation" in notebook "English practice"

  Scenario: Different assimilation pages for different notes
    When I assimilate these in sequence:
      | Assimilation Type | Title    | Additional Info             |
      | single note       | Sedition | Incite violence             |
      | single note       | Sedation | Put to sleep                |
      | image note        | Sedative | Sleep medicine; a_slide.jpg |
      | relationship      | Sedition | similar to; Sedation        |
    Then I should see the no more notes to assimilate toast

  Scenario: Count of recall and assimilate notes
    Given It's day 1, 8 hour
    And I assimilate these in sequence:
      | Assimilation Type | Title    |
      | single note       | Sedition |
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall
    And I should see 3 due for assimilation
