Feature: Initial Review
    As a learner, I want to review my notes. When notes are reviewed for the first time,
    there should be a daily max and I should be able to give some of my initial impression
    of the note, and/or choose to skip it for any further reviews.

    Background:
        Given I've logged in as an existing user
        And my daily new notes to review is set to 2
        And there are notes from Note 1 to Note 5

    Scenario: First day of review
        Given It's day 1, 8 hour
        Then I do these initial reviews in sequence:
            | review_type   | title      |
            | single note   | Note 1     |
            | single note   | Note 2     |
            | initial done  |            |
        Given It's day 1, 9 hour
        Then I do these initial reviews in sequence:
            | review_type   | title      |
            | initial done  |            |

    Scenario: First day review only part of the daily number
        Given It's day 1, 8 hour
        Then I do these initial reviews in sequence:
            | review_type   | title      |
            | single note   | Note 1     |
        Given It's day 1, 9 hour
        Then I do these initial reviews in sequence:
            | review_type   | title      |
            | single note   | Note 2     |
            | initial done  |            |

    Scenario: Follow schedule strictly for 2 days
        Given It's day 1, 8 hour
        Then I do these initial reviews in sequence:
            | review_type   | title      |
            | single note   | Note 1     |
            | single note   | Note 2     |
        Given It's day 2, 8 hour
        Then I do these initial reviews in sequence:
            | review_type   | title      |
            | single note   | Note 3     |

