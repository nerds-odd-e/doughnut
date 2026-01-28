Feature: Promote Point to Child Note
  As a learner, when I see understanding points in the assimilation page,
  I want to promote a point to become a child note,
  So that I can expand on that point as a separate note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Machine Learning" and details "Machine learning is a subset of artificial intelligence that enables systems to learn and improve from experience without being explicitly programmed."

  @usingMockedOpenAiService
  Scenario: Promote a point from understanding checklist to child note
    Given OpenAI generates understanding checklist with points:
      | Machine learning is a subset of artificial intelligence |
      | It enables systems to learn from experience |
      | Applications range from recommendation systems to autonomous vehicles |
    And OpenAI will extract point "It enables systems to learn from experience" to child note with title "Learning from Experience" and details "Systems improve their performance through experience" and updated parent details "Machine learning is a subset of AI. See child notes for details."
    When I start assimilating "Machine Learning"
    Then I should see an understanding checklist with a maximum of 5 points
    When I promote the point "It enables systems to learn from experience" to a child note
    Then I should remain on the assimilation page for "Machine Learning"
    And the point "It enables systems to learn from experience" should be removed from the understanding checklist
    And the understanding checklist should still show the remaining points
