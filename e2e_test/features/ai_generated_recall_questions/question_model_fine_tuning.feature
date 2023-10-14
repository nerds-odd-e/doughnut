@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training examples for fine-tuning OpenAI
  As an admin,
  I want the users to be able to suggest good note questions or improvements for bad ones,
  So that I can use these data for OpenAI fine-tuning to improve question generation.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "Who Let the Dogs Out"
    And OpenAI by default returns this question:
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |
    And I ask to generate a question for the note "Who Let the Dogs Out"


  Scenario: Admin should be able to generate training data from suggested questions
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin can download the question generation training data having "Who wrote 'Who Let the Dogs Out'?"
    Then an admin can download the question generation training data having "Baha Men"

  Scenario Outline: Admin should be able to generate training data from questions with good feedback
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a <Feedback> example
    Then I should see a message saying the feedback was sent successfully
    And an admin can download the question generation training data containing <Number_of_example_download> examples

    Examples:

      | Feedback | Number_of_example_download |
      | good     | 1                          |
      | bad      | 0                          |

  Scenario: Admin should be able to download both positive and negative feedbacks for training evaluation model
    Given I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    And I have a note with the topic "ChatGPT"
    And OpenAI by default returns this question:
      | Question Stem                      | Correct Choice | Incorrect Choice 1 |
      | In which year is ChatGPT launched? | 2002           | 2001               |
    And I ask to generate a question for the note "ChatGPT"
    And I suggest the displayed question "In which year is ChatGPT launched?" as a bad example
    Then an admin should be able to download the training data for evaluation containing 2 examples

  Scenario: User should not be able to submit response without a specific feedback
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" without feedback
    Then I should see a message saying the feedback was rejected
    And an admin should be able to download the training data for evaluation containing 0 examples

  Scenario: User should not be able to submit response again for the same question
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" with an existing feedback
    Then I should see a message saying the feedback already exist
    And an admin can download the question generation training data containing 1 examples

  Scenario: Admin should be able to edit the first question and choice suggested
    Given I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    When an admin edit the question and choices "Who wrote 'Who Let the Dogs Out'?" with a different question:
      | Question Stem                              | Choice A |
      | Did Baha Men write 'Who Let the Dogs Out'? | Yes      |
    Then an admin can download the question generation training data having "Did Baha Men write 'Who Let the Dogs Out'?"
    And an admin can download the question generation training data having "Yes"

  Scenario: Admin should be able to duplicate negative feedback
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a bad example
    Then an admin can duplicate the question "Who wrote 'Who Let the Dogs Out'?"
    And an admin should be able to see 2 examples containing "Who wrote 'Who Let the Dogs Out'?"
    And an admin should be able to identify the duplicated record

  Scenario: Admin should not be able to duplicate positive feedback
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin should not be able to duplicate this feedback to the question "Who wrote 'Who Let the Dogs Out'?"
