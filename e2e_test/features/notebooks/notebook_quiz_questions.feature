Feature: Notebook quiz questions

    Background:
        Given I am logged in as an existing user
        When  I have a notebook with head note "Scrum" and notes:
            | Topic              | Parent Topic              |
            | Scrum roles        | Scrum                     |
            | Scrum master       | Scrum roles               |
            | Scrum events       | Scrum                     |

    Scenario: As a note owner, I would like to approve question for assessment at notebook level
        When I add the following question for the note "Scrum master":
            | Stem                                                                      | Choice 0                      | Choice 1                              | Choice 2                              | Correct Choice Index |
            | How do you ensure effective daily Scrum meetings?                         | Rotating Scrum Master role    | Predefined agenda and time limits     | Extended discussions for all issues   | 1                    |
       Then I should be able to approve question "How do you ensure effective daily Scrum meetings?" on notebook "Scrum"