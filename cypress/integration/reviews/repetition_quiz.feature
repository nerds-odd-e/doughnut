Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description                    | skipReview | testingParent |
      | English  |                                | true       |               |
      | sedition | Sedition means incite violence | false      | English       |
      | sedation | Put to sleep is sedation       | false      | English       |
      | medical  |                                | true       | English       |

  Scenario: Auto generate cloze deletion
    Given I learned one note "sedition" on day 1
    And I link note "sedition" as "is tagged by" note "medical"
    When I am repeat-reviewing my old note on day 2
    Then I should be asked cloze deletion question "[...] means incite violence" with options "sedition, sedation"
    And On the current page, I should see "Sedition" has link "is tagged by" "medical"

  Scenario Outline: Answering cloze question
    Given I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    When I choose answer "<answer>"
    Then I should see that my answer <result>
    And I should see the satisfied button: "<should see the next button>"

    Examples:
      | answer   | result              | should see the next button |
      | sedation | "sedation" is wrong | no                         |
      | sedition | is correct          | yes                        |

  Scenario: View last result when the quiz answer was correct
    Given I learned one note "sedition" on day 1
    And I learned one note "sedation" on day 1
    When I am repeat-reviewing my old note on day 2
    And I choose answer "sedition"
    Then I view the last result
    And I should see that my answer is correct
    Then I should see "sedition" in note title
    And I should see the statistics of note "sedition"
      | RepetitionCount |
      | 1               |


  Scenario Outline: Spelling quiz
    Given I am learning new note on day 1
    And I have selected the option "Remember Spelling"
    When I am repeat-reviewing my old note on day 2
    Then I should be asked spelling question "means incite violence"
    When I type my answer "<answer>"
    Then I should see that my answer <result>

    Examples:
      | answer   | result          |
      | asdf     | "asdf" is wrong |
      | Sedition | is correct      |

  Scenario: Update review setting
    Given I am changing note "sedition"'s review setting
    And I have selected the option "Remember Spelling" in review setting
    When I am learning new note on day 1
    Then I should see the option "Remember Spelling" is "on"
    When I have unselected the option "Remember Spelling"
    And I am changing note "sedition"'s review setting
    Then I should see the option "Remember Spelling" is "off"

