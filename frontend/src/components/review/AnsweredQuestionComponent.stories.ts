import type { Meta, StoryObj } from "@storybook/vue3"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import makeMe from "@tests/fixtures/makeMe"
import type { AnsweredQuestion } from "@generated/backend"

const meta = {
  title: "Review/AnsweredQuestionComponent",
  component: AnsweredQuestionComponent,
  tags: ["autodocs"],
  argTypes: {
    answeredQuestion: {
      control: "object",
    },
    conversationButton: {
      control: "boolean",
    },
  },
} satisfies Meta<typeof AnsweredQuestionComponent>

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

// Correctly answered question
export const CorrectAnswer: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion(
      "What is the capital of France?",
      ["Paris", "London", "Berlin", "Madrid"],
      0,
      0,
      true
    ),
    conversationButton: true,
  },
}

// Incorrectly answered question
export const IncorrectAnswer: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion(
      "What is the capital of France?",
      ["Paris", "London", "Berlin", "Madrid"],
      0,
      1,
      false
    ),
    conversationButton: true,
  },
}

// Question with explanation/metadata (using a note)
export const WithNote: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion(
        "What is TypeScript?",
        ["A programming language", "A database", "A framework", "A browser"],
        0,
        0,
        true
      )
      question.note = makeMe.aNote
        .topicConstructor("TypeScript")
        .details(
          "TypeScript is a typed superset of JavaScript that compiles to plain JavaScript."
        )
        .please()
      return question
    })(),
    conversationButton: true,
  },
}

// Question in a sequence (showing multiple questions)
export const InSequence: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion(
        "Which of the following is a JavaScript framework?",
        ["React", "Python", "Java", "C++"],
        0,
        0,
        true
      )
      question.recallPromptId = 1
      return question
    })(),
    conversationButton: true,
  },
}

// Question without conversation button
export const WithoutConversationButton: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion(
      "What is 2 + 2?",
      ["3", "4", "5", "6"],
      1,
      1,
      true
    ),
    conversationButton: false,
  },
}

// Question with custom question stem and choices
export const CustomQuestion: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion(
        "Which data structure follows LIFO principle?",
        ["Queue", "Stack", "Array", "Linked List"],
        1,
        2,
        false
      )
      question.note = makeMe.aNote
        .topicConstructor("Data Structures")
        .details(
          "A stack is a linear data structure that follows the Last In First Out (LIFO) principle."
        )
        .please()
      return question
    })(),
    conversationButton: true,
  },
  decorators: [
    () => ({
      template:
        '<div style="max-width: 800px; margin: 0 auto; padding: 20px;"><story /></div>',
    }),
  ],
}
