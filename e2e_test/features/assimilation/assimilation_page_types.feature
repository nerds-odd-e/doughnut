Feature: Assimilation page types
  As a learner, I want the assimilation page to show the right layout for
  the note I'm assimilating, whether it's a plain note, an image, or a
  relationship between two notes.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content         | Image Url   |
      | English  |                 |             |
      | Sedition | Incite violence |             |
      | Sedation | Put to sleep    |             |
      | Sedative | Sleep medicine  | a_slide.jpg |
    And the notes "English" are skipped from the assimilation sequence
    And there is "similar to" relationship between note "Sedition" and "Sedation" in notebook "English practice"

  Scenario: Different assimilation pages for different notes
    When I assimilate these in sequence:
      | Assimilation Type | Title    | Additional Info             |
      | single note       | Sedition | Incite violence             |
      | single note       | Sedation | Put to sleep                |
      | image note        | Sedative | Sleep medicine; a_slide.jpg |
      | relationship      | Sedition | similar to; Sedation        |
    Then I should see the no more notes to assimilate toast
