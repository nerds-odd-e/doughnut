Feature: Repetition
    As a learner, I want to be tested in repetition and also update my memory status.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title      | description                    | skipReview  | testingParent  |
            | English    |                                | true        |                |
            | sedition   | Sedition means incite violence | false       | English        |
            | sedation   | Put to sleep is sedation       | false       | English        |

    Scenario Outline: : Auto generate cloze deletion
        Given I learned one note "sedition" on day 1
        When I repeat reviewing my old note on day 2
        Then I should be asked cloze deletion question "[...] means incite violence" with options "sedition, sedation"
        When I choose answer "<answer>"
        Then I should see that my answer <result>

        Examples:
        | answer   | result               |
        | sedation | "sedation" is wrong  |
        | sedition | is correct           |

