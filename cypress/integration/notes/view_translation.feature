Feature: View Translate on Notes
    As a user I want to see translation of the Note
    

        @ignore
    Scenario: Should show translation button
        Given I am on Notes List
        When I click one of Notes
        Then Notes details will be shownn in ENG version (as default) and button ID will be shown

        @ignore
    Scenario: Should translate note's content
        Given I am on Notes Detail
        When I click on ID/ENG button and ID/ENG translation is available
        Then Title and Description for Notes and its children will be translated to ID/ENG