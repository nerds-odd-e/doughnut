import type { Meta, StoryObj } from "@storybook/vue3"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"
import makeMe from "@tests/fixtures/makeMe"
import type { AnsweredQuestion } from "@generated/backend"

const meta = {
  title: "Page Views/MemoryTrackerPageView",
  component: MemoryTrackerPageView,
  tags: ["autodocs"],
  argTypes: {
    answeredQuestion: {
      control: "object",
    },
  },
} satisfies Meta<typeof MemoryTrackerPageView>

export default meta
type Story = StoryObj<typeof meta>

// Helper to create an answered question with custom predefined question
const createAnsweredQuestionWithQuestion = (
  stem: string,
  choices: string[],
  correctIndex: number,
  answerIndex: number,
  isCorrect: boolean
): AnsweredQuestion => {
  const predefinedQuestion = makeMe.aPredefinedQuestion
    .withQuestionStem(stem)
    .withChoices(choices)
    .correctAnswerIndex(correctIndex)
    .please()

  const baseQuestion = makeMe.anAnsweredQuestion
    .answerCorrect(isCorrect)
    .withChoiceIndex(answerIndex)
    .please()

  return {
    ...baseQuestion,
    predefinedQuestion,
    answerDisplay: choices[answerIndex] || "",
  }
}

// With an answered question
export const WithAnsweredQuestion: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion(
        "What is the capital of France?",
        ["Paris", "London", "Berlin", "Madrid"],
        0,
        0,
        true
      )
      question.note = makeMe.aNote
        .topicConstructor("France")
        .details(
          "France is a country in Western Europe. Paris is its capital and largest city."
        )
        .please()
      return question
    })(),
  },
}

// With incorrect answer
export const WithIncorrectAnswer: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion(
        "What is the capital of France?",
        ["Paris", "London", "Berlin", "Madrid"],
        0,
        1,
        false
      )
      question.note = makeMe.aNote
        .topicConstructor("France")
        .details(
          "France is a country in Western Europe. Paris is its capital and largest city."
        )
        .please()
      return question
    })(),
  },
}

// Question with note that has many ancestors
export const NoteWithManyAncestors: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion(
        "What is TypeScript?",
        ["A programming language", "A database", "A framework", "A browser"],
        0,
        0,
        true
      )

      // Create a chain of 10 ancestor notes
      let currentNote = makeMe.aNote.topicConstructor("Ancestor 1").please()
      for (let i = 2; i <= 10; i++) {
        currentNote = makeMe.aNote
          .topicConstructor(`Ancestor ${i}`)
          .underNote(currentNote)
          .please()
      }

      // Create the final note with all ancestors
      question.note = makeMe.aNote
        .topicConstructor("TypeScript")
        .underNote(currentNote)
        .details(
          "TypeScript is a typed superset of JavaScript that compiles to plain JavaScript."
        )
        .please()
      return question
    })(),
  },
}

// No answered question found
export const NoQuestionFound: Story = {
  args: {
    answeredQuestion: null,
  },
}
