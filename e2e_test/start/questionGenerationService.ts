import mock_services from "./mock_services"

export const questionGenerationService = () => ({
  stubAskSingleAnswerMultipleChoiceQuestion: (record: Record<string, string>) => {
    const reply = JSON.stringify({
      stem: record["Question Stem"],
      correctChoiceIndex: 0,
      choices: [
        record["Correct Choice"],
        record["Incorrect Choice 1"],
        record["Incorrect Choice 2"],
      ],
    })
    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await mock_services
        .openAi()
        .stubAnyChatCompletionFunctionCall("ask_single_answer_multiple_choice_question", reply)
    })
  },
})
