import type { Meta, StoryObj } from "@storybook/vue3"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"
import makeMe from "@tests/fixtures/makeMe"

const meta = {
  title: "Page Views/MemoryTrackerPageView",
  component: MemoryTrackerPageView,
  tags: ["autodocs"],
  argTypes: {
    recallPrompts: {
      control: "object",
    },
  },
} satisfies Meta<typeof MemoryTrackerPageView>

export default meta
type Story = StoryObj<typeof meta>

// Helper to create a recall prompt with answer
const createRecallPromptWithAnswer = (
  stem: string,
  choices: string[],
  correctIndex: number,
  answerIndex: number,
  isCorrect: boolean
) => {
  const note = makeMe.aNote
    .topicConstructor("France")
    .details(
      "France is a country in Western Europe. Paris is its capital and largest city."
    )
    .please()

  const predefinedQuestion = makeMe.aPredefinedQuestion
    .withQuestionStem(stem)
    .withChoices(choices)
    .correctAnswerIndex(correctIndex)
    .please()

  return makeMe.aRecallPrompt
    .withNote(note)
    .withPredefinedQuestion(predefinedQuestion)
    .withAnswer({
      id: 1,
      correct: isCorrect,
      choiceIndex: answerIndex,
    })
    .withAnswerTime(new Date().toISOString())
    .please()
}

// With an answered question
export const WithAnsweredQuestion: Story = {
  args: {
    recallPrompts: [
      createRecallPromptWithAnswer(
        "What is the capital of France?",
        ["Paris", "London", "Berlin", "Madrid"],
        0,
        0,
        true
      ),
    ],
    memoryTrackerId: 1,
  },
}

// With incorrect answer
export const WithIncorrectAnswer: Story = {
  args: {
    recallPrompts: [
      createRecallPromptWithAnswer(
        "What is the capital of France?",
        ["Paris", "London", "Berlin", "Madrid"],
        0,
        1,
        false
      ),
    ],
    memoryTrackerId: 1,
  },
}

// Question with note that has many ancestors
export const NoteWithManyAncestors: Story = {
  args: {
    recallPrompts: [
      (() => {
        // Create a chain of 10 ancestor notes
        let currentNote = makeMe.aNote.topicConstructor("Ancestor 1").please()
        for (let i = 2; i <= 10; i++) {
          currentNote = makeMe.aNote
            .topicConstructor(`Ancestor ${i}`)
            .underNote(currentNote)
            .please()
        }

        // Create the final note with all ancestors
        const note = makeMe.aNote
          .topicConstructor("TypeScript")
          .underNote(currentNote)
          .details(
            "TypeScript is a typed superset of JavaScript that compiles to plain JavaScript."
          )
          .please()

        const predefinedQuestion = makeMe.aPredefinedQuestion
          .withQuestionStem("What is TypeScript?")
          .withChoices([
            "A programming language",
            "A database",
            "A framework",
            "A browser",
          ])
          .correctAnswerIndex(0)
          .please()

        return makeMe.aRecallPrompt
          .withNote(note)
          .withPredefinedQuestion(predefinedQuestion)
          .withAnswer({
            id: 1,
            correct: true,
            choiceIndex: 0,
          })
          .withAnswerTime(new Date().toISOString())
          .please()
      })(),
    ],
    memoryTrackerId: 1,
  },
}

// No recall prompts found
export const NoQuestionFound: Story = {
  args: {
    recallPrompts: [],
    memoryTrackerId: 1,
  },
}
