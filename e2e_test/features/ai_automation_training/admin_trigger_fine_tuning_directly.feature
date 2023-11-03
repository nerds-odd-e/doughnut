@usingMockedOpenAiService
Feature: Trigger fine-tuning directly as admin

  As an admin,
  I want to trigger fine-tuning directly through 1 button click without manually uploading feedback examples.

  Background:
    Given there is a fine-tuning file "question_gerenation_examples" on my OpenAI account
    Given I am logged in as an admin and click AdminDashboard and go to tab "Fine Tuning Data"
    And I've got the following question for a note with topic "Who Let the Dogs Out":
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |


  Scenario Outline: Trigger fine tuning with feedbacks for Question model and Evaluation model
    Given I have <positive_count> positive feedbacks and <negative_count> negative feedbacks
    And OpenAI response "<upload_result>" when uploading fine tuning data
    And OpenAi response "<training_result>" when trigger fine tuning data
    When I trigger fine tuning
    Then I should see the message <message>
    Examples:
      | positive_count | negative_count | message                                     | upload_result | training_result |
      | 9              | 9              | "Positive feedback cannot be less than 10." | success       | success         |
      | 10             | 10             | "Upload failed."                                        | failed        | success        |
      | 10             | 10             | "Training failed."                                      | success       | failed         |
      | 10             | 10             | "Training is in progress."                  | success       | success         |
