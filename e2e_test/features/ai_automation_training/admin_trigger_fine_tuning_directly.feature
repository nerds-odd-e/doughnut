@usingMockedOpenAiService
Feature: Trigger fine-tuning directly as admin

  As an admin,
  I want to trigger fine-tuning directly through 1 button click without manually uploading feedback examples.

  Background:
#    Given there is a fine-tuning file "question_gerenation_examples" on my OpenAI account
    Given I am logged in as an admin and click AdminDashboard and go to tab "Fine Tuning Data"

#  Scenario Outline:
#      Given the finetuning for the file "question_generation_examples" will be "<API response>"
#      When I retrieve file list from my openAI account
#      Then I will see a list of files
#      When I choose the file "question generation examples"
#      And I train model with "question generation examples" data based on GPT3.5 model
#      Then I will see success message "<expected message>"
#
#    Examples:
#    | API response | expected message |
#    | Successful   | Successful       |
#    | Failed       | Failed           |

  @ignore
Scenario Outline: make upload and training progress
  Given I have <positive_count> positive feedbacks and <negative_count> negative feedbacks
  And OpenAI response "<upload_result>" when uploading fine tuning data
  And OpenAi response "<training_result>" when trigger fine tuning data
  When I click on the "Trigger Fine Tuning" button
  Then I should see the message <message>
  Examples:
    | positive_count | negative_count | message                                                | upload_result | training_result |
#    | 9              | 9              | "You need at least 10 feedbacks to train the AI model" | success       | success        |
#    | 10             | 10             | "Upload failed"                                        | failed        | success        |
#    | 10             | 10             | "Training failed"                                      | success       | failed         |
    | 10             | 10             | "Training is in progress"                              | success       | success        |
