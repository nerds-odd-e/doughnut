import type { Notebook, RecallPrompt } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'

export function mcqRecallPrompt(
  id: number,
  stem: string,
  choices: string[]
): RecallPrompt {
  return {
    ...makeMe.aRecallPrompt
      .withId(id)
      .withQuestionStem(stem)
      .withChoices(choices)
      .please(),
    notebook: undefined,
  }
}

export function mcqRecallPromptWithNotebook(
  id: number,
  stem: string,
  choices: string[],
  notebook: Notebook
): RecallPrompt {
  return {
    ...makeMe.aRecallPrompt
      .withId(id)
      .withQuestionStem(stem)
      .withChoices(choices)
      .please(),
    notebook,
  }
}

export function spellingRecallPrompt(id: number, stem: string): RecallPrompt {
  return {
    ...makeMe.aRecallPrompt.withId(id).withSpellingStem(stem).please(),
    notebook: undefined,
  }
}

export function spellingRecallPromptWithNotebook(
  id: number,
  stem: string,
  notebook: Notebook
): RecallPrompt {
  return {
    ...makeMe.aRecallPrompt.withId(id).withSpellingStem(stem).please(),
    notebook,
  }
}
