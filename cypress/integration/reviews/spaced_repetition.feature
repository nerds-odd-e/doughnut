Feature: Spaced-repetition
    As a learner, I want to review my notes in the most optimized way

    Background:
        Given I've logged in as an existing user
        And my daily new notes to review is set to 1
        And my space setting is "1, 2, 4, 8"
        Given there are notes from Note 1 to Note 3

    Scenario: Strictly follow the schedule
        * On day 1 at 8 hour I repeat old "                " and at 8 hour initial review new "Note 1   "
        * On day 2 at 8 hour I repeat old "Note 1          " and at 8 hour initial review new "Note 2   "
        * On day 3 at 8 hour I repeat old "Note 2          " and at 8 hour initial review new "Note 3   "
#            | 4    | 1, 3                |                          |
#            | 5    | 2                   |                          |
#            | 6    |                     |                          |
#            | 7    | 3                   |                          |
#            | 8    | 1                   |                          |

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

