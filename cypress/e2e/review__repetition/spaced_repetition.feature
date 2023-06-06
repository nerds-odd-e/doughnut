Feature: Spaced-repetition
    As a learner, I want to review my notes in the most optimized way

    Background:
        Given I've logged in as an existing user
        And my daily new notes to review is set to 1
        And my space setting is "1, 2, 4, 8"
        Given there are notes from Note 1 to Note 3

    @mockBrowserTime
    Scenario: The review page
        Given On day 1 I should have "1/3" note for initial review and "0/0" for repeat
        When I initial review "Note 1"
        Then On day 1 I should have "0/2" note for initial review and "0/1" for repeat
        And On day 2 I should have "1/2" note for initial review and "1/1" for repeat

    @mockBrowserTime
    Scenario: Strictly follow the schedule
        * On day 1 I repeat old "                    " and initial review new "Note 1, end "
        * On day 2 I repeat old "Note 1, end         " and initial review new "Note 2, end "
        * On day 3 I repeat old "Note 2, end         " and initial review new "Note 3, end "
        * On day 4 I repeat old "Note 1, Note 3, end " and initial review new "end         "
        * On day 5 I repeat old "Note 2, end         " and initial review new "end         "
        * On day 6 I repeat old "Note 3, end         " and initial review new "end         "
        * On day 7 I repeat old "                    " and initial review new "end         "
        * On day 8 I repeat old "Note 1, end         " and initial review new "end         "

    @mockBrowserTime
    Scenario: Strictly follow the schedule but want to review more
        * On day 1 I repeat old "                    " and initial review new "Note 1, end "
        * On day 2 I repeat old "Note 1, end         " and initial review new "Note 2, end "
        * On day 3 I repeat old "Note 2, end         " and initial review new "Note 3, end "
        Given I ask to do more repetition
        When I repeat more old "Note 1         "
        Then On day 4 I should have "0/0" note for initial review and "1/3" for repeat
