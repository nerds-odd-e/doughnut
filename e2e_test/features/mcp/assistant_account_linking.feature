Feature: Assistant account linking
  A hosted assistant can read my Donut knowledge after I grant access through GitHub.

  Scenario: A hosted SDK assistant renews access until I withdraw permission
    Given there are some notes for existing user "old_learner" in notebook "SDK Notebook"
      | Title             | Content                                         |
      | Hosted SDK Marker | Initial SDK authorization reads this private note |
      | Hosted SDK Sibling | Nearby private knowledge                         |
    When I connect the hosted assistant through GitHub and allow read access
    Then the hosted assistant reads "Hosted SDK Marker" containing "Initial SDK authorization reads this private note"
    And the hosted assistant explores bounded graph context for "Hosted SDK Marker"
    When the hosted assistant's access expires
    Then the hosted assistant renews automatically and reads the same private note
    When I withdraw the hosted assistant in Settings
    Then the existing hosted assistant connection loses access and renewal
    When I reconnect the hosted assistant
    Then Donut asks for informed consent again
