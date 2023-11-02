Feature: Upload fine tuning data


  Background:
    Given I am logged in as an existing user
    And I've got the following question for a note with topic "Who Let the Dogs Out":
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |

  Scenario Outline: Block upload fine tuning data
    Given I have <positive_count> positive feedbacks and <negative_count> negative feedbacks
    When I upload the feedbacks
    Then I should see the message "Positive feedback cannot be less than 10."

    Examples:
      | positive_count | negative_count |
      | 9              | 0              |
      | 9              | 1              |
      | 0              | 10             |
      | 9              | 10             |

#  @ignore
  @usingMockedOpenAiService
  Scenario: Open AI fail
    Given An OpenAI response is unavailable
    Given I have 10 positive feedbacks and 1 negative feedbacks
    When I upload the feedbacks
    Then I should see the message "Something wrong with Open AI service."

  @ignore
  @usingMockedOpenAiService
  Scenario Outline: Upload fine tuning data to Open AI service
    Given I have <positive_count> positive feedbacks and <negative_count> negative feedbacks
    When I upload the feedbacks
    Then I should see the message "Upload successfully."
    Then Open AI service should receive the uploaded file.

    Examples:
      | positive_count | negative_count |
      | 10             | 0              |
      | 11             | 10             |
