@ignore
Feature: Get approved questions
  As a learner, I want to do assessment for a notebook with only approved questions.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | parentTopic |
      | Animals          |             |
      | Zebra            | Animals     |
      | Flamingo         | Animals     |
      | Elephant         | Animals     |
      | Kitty cat        | Animals     |
      | Merlion          | Animals     |
      | Unicorn          | Animals     |

  Scenario Outline: Generating an assessment with varying number of approved questions
    Given that there are "<num_approved>" approved questions from at least 5 different notes in a notebook
    When generating an assessment
    Then should <be_able_to_generate> able to generate the assessment with 5 randomly selected approved questions

    Examples:
      | num_approved | be_able_to_generate |
      | 1            | not be              |
      | 5            | be                  |
      | 6            | be                  |

  Scenario: Fail to generate an assessment with sufficient approved questions but insufficient notes
    Given that there are 5 approved questions from only 4 different notes in a notebook
    When generating an assessment
    Then should not be able to generate the assessment with 5 randomly selected approved questions
