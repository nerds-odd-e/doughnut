Feature: Spaced-repetition
    As a learner, I want to review my notes in the most optimized way

    Background:
        Given I've logged in as an existing user
        And my daily new notes to review is set to 1
        And my space setting is "1, 2, 4, 8"
        Given there are notes from Note 1 to Note 3

    Scenario: Strictly follow the schedule
        * On day 1 I repeat old "                " and initial review new "Note 1   "
        * On day 2 I repeat old "Note 1          " and initial review new "Note 2   "
        * On day 3 I repeat old "Note 2          " and initial review new "Note 3   "
        * On day 4 I repeat old "Note 1, Note 3  " and initial review new "         "
        * On day 5 I repeat old "Note 2          " and initial review new "         "
        * On day 6 I repeat old "                " and initial review new "         "
        * On day 7 I repeat old "Note 3          " and initial review new "         "
        * On day 8 I repeat old "Note 1          " and initial review new "         "

    @ignore
    Scenario: Not strictly follow the schedule
        Then Review in sequence
            | day  | old review | new review  |
            | 1    |            | 1, 2        |
            | 2    | 1          |             |
            | 3    | 2          | 3           |
            | 4    | 1, 2       |             |

    @ignore
    Scenario: I want to learn more new notes
        Then Review sequence
            | day  | old review | new review  | learn more  |
            | 1    |            | 1, 2        |             |
            | 2    | 1, 2       | 3, 4        | 5           |
            | 3    | 3, 4, 5    |             |             |

    @ignore
    Scenario: I want to review more
        Then Review sequence
            | day  | old review | new review  | review more |
            | 1    |            | 1, 2        |             |
            | 2    | 1, 2       | 3, 4        | 1, 2, 3, 4  |
            | 3    | 3, 4       | 5           |             |
            | 4    | 1, 2, 5    |             |             |

