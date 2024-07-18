Feature: Notebook quiz questions

    Background:
        Given I am logged in as an existing user
        When I have a notebook with head note "Scrum" and notes:
            | Topic              | Parent Topic              |
            | Scrum roles        | Scrum                     |
            | Scrum master       | Scrum roles               |
            | Scrum events       | Scrum                     |

    Scenario: View question when there is no question on each note
        Then I should see empty question list on notebook "Scrum"

    @ignore
    Scenario: View question that add at primary level notebook
        When I add the following question for the note "Scrum":
            | Stem                          | Choice 0          | Choice 1        | Choice 2         | Correct Choice Index |
            | What's the scrum values?      | Focus             | Fun             | Fake             | 0                    |
        Then I should see the following question on notebook "Scrum":
            | Notebook                      | Question                       | Correct Choice       |
            | Scrum                         | What's the scrum values?       | 0                    |

    @ignore
    Scenario: View question that add from child level note
        When I add the following question for the note "Scrum roles":
            | Stem                                          | Choice 0         | Choice 1       | Choice 2          | Correct Choice Index |
            | Which one not in scrum team?                  | Scrum master     | The developer  | Project manager   | 2                    |
            | How many people in the scrum team?            | Less than 10     | 7              | 5                 | 0                    |
        And I add the following question for the note "Scrum master":
            | Stem                                                                      | Choice 0                      | Choice 1                              | Choice 2                              | Correct Choice Index |
            | How do you ensure effective daily Scrum meetings?                         | Rotating Scrum Master role    | Predefined agenda and time limits     | Extended discussions for all issues   | 1                    |
            | Who is reponsible for ensuring that the Scrum framework is understood?    | Scrum Master                  | Product Owner                         | The Developer                         | 0                    |
        Then I should see the following question on notebook "Scrum":
            | Notebook                       | Question                                                                 | Correct Choice       |
            | Scrum roles                    | Which one not in scrum roles?                                            | 2                    |
            | Scrum roles                    | How many people in the scrum team?                                       | 0                    |
            | Scrum master                   | How do you ensure effective daily Scrum meetings?                        | 1                    |
            | Scrum master                   | Who is reponsible for ensuring that the Scrum framework is understood?   | 0                    |