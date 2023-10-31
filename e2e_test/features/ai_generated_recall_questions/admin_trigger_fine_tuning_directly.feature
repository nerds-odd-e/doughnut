@ignore
@usingMockedOpenAiService
Feature: Trigger fine-tuning directly as admin

  As an admin,
  I want to trigger fine-tuning directly through 1 button click without accessing OpenAI website

  Background:
    Given I am logged in as an existing user
    And Having uploaded training data already with filename 'temp.jsonl' on OpenAI website:

  Scenario: Admin can select file from dropdown list
    When I click the dropdown list
    Then I will see 'temp.jsonl' in the list

  Scenario: Admin can trigger fine-tuning
    When I select 'temp.jsonl' and click tirgger button
    Then I will see success message
