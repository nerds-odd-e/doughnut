@disableOpenAiService
Feature: Spaced-repetition
    As a learner, I want to review my notes in the most optimized way

    Background:
        Given I am logged in as an existing user
        And my daily new notes to review is set to 1
        And my space setting is "1, 2, 4, 8"
        Given there are notes from Note 1 to Note 3

    @mockBrowserTime
    Scenario: The review page
        Given On day 1 I should have "0/1/3" note for assimilation and "0/0/0" for recall
        When I assimilate "Note 1"
        Then On day 1 I should have "1/1/3" note for assimilation and "0/0/1" for recall
        And On day 3 I should have "1/2/3" note for assimilation and "0/0/1" for recall

    @mockBrowserTime
    Scenario: Strictly follow the schedule
        * On day 1 I recall "                    " and assimilate new "Note 1, end "
        * On day 2 I recall "Note 1, end         " and assimilate new "Note 2, end "
        * On day 3 I recall "Note 2, end         " and assimilate new "Note 3, end "
        * On day 4 I recall "Note 1, Note 3, end " and assimilate new "end         "
        * On day 5 I recall "Note 2, end         " and assimilate new "end         "
        * On day 6 I recall "Note 3, end         " and assimilate new "end         "
        * On day 7 I recall "                    " and assimilate new "end         "
        * On day 8 I recall "Note 1, end         " and assimilate new "end         "

    @mockBrowserTime
    Scenario: Strictly follow the schedule but want to review more
        * On day 1 I recall "                    " and assimilate new "Note 1, end "
        * On day 2 I recall "Note 1, end         " and assimilate new "Note 2, end "
        * On day 3 I recall "Note 2, end         " and assimilate new "Note 3, end "
        Given I ask to do more repetition
        When I repeat more old "Note 1         "
        Then On day 4 I should have "3/3/3" note for assimilation and "3/5/3" for recall
