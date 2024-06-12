Feature: Approve Quiz Question
  As an owner, I want to approve the quiz questions for the notes of a notebook,
  so that I can use these questions for assessment.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "The cow joke"
    And there are questions for the note:
      | noteTopic    | question             | answer | oneWrongChoice | approved |
      | The cow joke | What does a cow say? | moo    | woo            | false    |

  Scenario: Approve quiz question
    When I approve question "What does a cow say?" of topic "The cow joke":
      | question             | Approved |
      | What does a cow say? | true     |
    Then I should see the approved questions in the question list of the note "The cow joke":
      | Question             | Approved |
      | What does a cow say? | true     |

  @ignore
  Scenario: Approve all quiz question of topic
    When I approve question "What does a cow say?" of topic "The cow joke":
      | question             | approved |
      | What does a cow say? | true     |
    Then I should see the questions in the question list of the note "The cow joke":
      | Question             | approved |
      | What does a cow say? | true     |

  @ignore
  Scenario: UnApprove quiz question
    When I unapprove question "What does a cow say?" of topic "The cow joke":
      | question             | approved |
      | What does a cow say? | true     |
    Then I should see the questions in the question list of the note "The cow joke":
      | Question             | approved |
      | What does a cow say? | true     |
