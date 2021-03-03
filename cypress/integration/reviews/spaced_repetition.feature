Feature: Spaced-repetition
    As a learner, I want to review my notes in the most optimized way

    Background:
        Given I've logged in as an existing user
        And my daily new notes to review is set to 2
        And my space setting is "1, 2, 4, 8"
        Given there are notes from Note 1 to Note 5

    Scenario: First day of review
        Then Reviews should have review pages in sequence:
            | review type   | title      |
            | single note   | Note 1     |
            | single note   | Note 2     |
            | end of review |            |

    @ignore
    Scenario: Strictly follow the schedule
        Then Review sequence
            | days | old notes to review | new notes to review  |
            | 1    |                     | 1, 2                 |
            | 2    | 1, 2                | 3, 4                 |
            | 3    | 3, 4                | 5                    |
            | 4    | 1, 2, 5             |                      |
            | 5    | 3, 4                |                      |
            | 6    |                     |                      |
            | 7    | 5                   |                      |
            | 8    | 1, 2                |                      |

    @ignore
    Scenario: Not strictly follow the schedule
        Then Review sequence
            | days | old review | new review  |
            | 1    |            | 1, 2        |
            | 2    | 1          |             |
            | 3    | 2          | 3           |
            | 4    | 1, 2       |             |

    @ignore
    Scenario: I want to learn more new notes
        Then Review sequence
            | days | old review | new review  | learn more  |
            | 1    |            | 1, 2        |             |
            | 2    | 1, 2       | 3, 4        | 5           |
            | 3    | 3, 4, 5    |             |             |

    @ignore
    Scenario: I want to review more
        Then Review sequence
            | days | old review | new review  | review more |
            | 1    |            | 1, 2        |             |
            | 2    | 1, 2       | 3, 4        | 1, 2, 3, 4  |
            | 3    | 3, 4       | 5           |             |
            | 4    | 1, 2, 5    |             |             |

