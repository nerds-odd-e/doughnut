import type { Meta, StoryObj } from "@storybook/vue3-vite"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import type { RecallPrompt } from "@generated/doughnut-backend-api"

const meta = {
  title: "Recall/AnsweredQuestionComponent",
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

const createAnsweredQuestionWithQuestion = (opts: {
  stem: string
  choices: string[]
  correctIndex: number
  answerIndex: number
  isCorrect: boolean
}): RecallPrompt =>
  makeMe.aRecallPrompt
    .withPredefinedQuestion(
      makeMe.aPredefinedQuestion
        .withQuestionStem(opts.stem)
        .withChoices(opts.choices)
        .correctAnswerIndex(opts.correctIndex)
        .please()
    )
    .withNote(makeMe.aNote.please())
    .withAnswer({
      id: 1,
      correct: opts.isCorrect,
      choiceIndex: opts.answerIndex,
    })
    .withId(1)
    .please()

export const CorrectAnswer: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion({
        stem: "What is the capital of France?",
        choices: ["Paris", "London", "Berlin", "Madrid"],
        correctIndex: 0,
        answerIndex: 0,
        isCorrect: true,
      })
      return {
        ...question,
        note: makeMe.aNote
          .title("France")
          .details(
            "France is a country in Western Europe. Paris is its capital and largest city."
          )
          .please(),
      }
    })(),
    conversationButton: true,
  },
}

export const IncorrectAnswer: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion({
        stem: "What is the capital of France?",
        choices: ["Paris", "London", "Berlin", "Madrid"],
        correctIndex: 0,
        answerIndex: 1,
        isCorrect: false,
      })
      return {
        ...question,
        note: makeMe.aNote
          .title("France")
          .details(
            "France is a country in Western Europe. Paris is its capital and largest city."
          )
          .please(),
      }
    })(),
    conversationButton: true,
  },
}

export const NoteWithManyAncestors: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion({
        stem: "What is TypeScript?",
        choices: [
          "A programming language",
          "A database",
          "A framework",
          "A browser",
        ],
        correctIndex: 0,
        answerIndex: 0,
        isCorrect: true,
      })

      let currentNote = makeMe.aNote.title("Ancestor 1").please()
      for (let i = 2; i <= 10; i++) {
        currentNote = makeMe.aNote
          .title(`Ancestor ${i}`)
          .underNote(currentNote)
          .please()
      }

      return {
        ...question,
        note: makeMe.aNote
          .title("TypeScript")
          .underNote(currentNote)
          .details(
            "TypeScript is a typed superset of JavaScript that compiles to plain JavaScript."
          )
          .please(),
      }
    })(),
    conversationButton: true,
  },
}

export const InSequence: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion({
        stem: "Which of the following is a JavaScript framework?",
        choices: ["React", "Python", "Java", "C++"],
        correctIndex: 0,
        answerIndex: 0,
        isCorrect: true,
      })
      return {
        ...question,
        id: 1,
        note: makeMe.aNote
          .title("React")
          .details(
            "React is a JavaScript library for building user interfaces, particularly web applications."
          )
          .please(),
      }
    })(),
    conversationButton: true,
  },
}

export const WithoutConversationButton: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion({
        stem: "What is 2 + 2?",
        choices: ["3", "4", "5", "6"],
        correctIndex: 1,
        answerIndex: 1,
        isCorrect: true,
      })
      return {
        ...question,
        note: makeMe.aNote
          .title("Basic Arithmetic")
          .details(
            "Addition is one of the four basic operations of arithmetic. 2 + 2 equals 4."
          )
          .please(),
      }
    })(),
    conversationButton: false,
  },
}

export const CustomQuestion: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion({
        stem: "Which data structure follows LIFO principle?",
        choices: ["Queue", "Stack", "Array", "Linked List"],
        correctIndex: 1,
        answerIndex: 2,
        isCorrect: false,
      })
      return {
        ...question,
        note: makeMe.aNote
          .title("Data Structures")
          .details(
            "A stack is a linear data structure that follows the Last In First Out (LIFO) principle."
          )
          .please(),
      }
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
