import type { Meta, StoryObj } from "@storybook/vue3-vite"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"

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
  noteTitle: string
}): AnsweredQuestion => {
  const note = makeMe.aNote.title(opts.noteTitle).please()
  return makeMe.anAnsweredQuestion
    .withNote(note)
    .withPredefinedQuestion(
      makeMe.aPredefinedQuestion
        .withQuestionStem(opts.stem)
        .withChoices(opts.choices)
        .correctAnswerIndex(opts.correctIndex)
        .please()
    )
    .withAnswer({
      id: 1,
      correct: opts.isCorrect,
      choiceIndex: opts.answerIndex,
    })
    .withId(1)
    .withMemoryTrackerId(1)
    .please()
}

export const CorrectAnswer: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion({
      stem: "What is the capital of France?",
      choices: ["Paris", "London", "Berlin", "Madrid"],
      correctIndex: 0,
      answerIndex: 0,
      isCorrect: true,
      noteTitle: "France",
    }),
    conversationButton: true,
  },
}

export const IncorrectAnswer: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion({
      stem: "What is the capital of France?",
      choices: ["Paris", "London", "Berlin", "Madrid"],
      correctIndex: 0,
      answerIndex: 1,
      isCorrect: false,
      noteTitle: "France",
    }),
    conversationButton: true,
  },
}

export const NoteWithManyAncestors: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion({
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
      noteTitle: "TypeScript",
    }),
    conversationButton: true,
  },
}

export const InSequence: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion({
      stem: "Which of the following is a JavaScript framework?",
      choices: ["React", "Python", "Java", "C++"],
      correctIndex: 0,
      answerIndex: 0,
      isCorrect: true,
      noteTitle: "React",
    }),
    conversationButton: true,
  },
}

export const WithoutConversationButton: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion({
      stem: "What is 2 + 2?",
      choices: ["3", "4", "5", "6"],
      correctIndex: 1,
      answerIndex: 1,
      isCorrect: true,
      noteTitle: "Basic Arithmetic",
    }),
    conversationButton: false,
  },
}

export const CustomQuestion: Story = {
  args: {
    answeredQuestion: createAnsweredQuestionWithQuestion({
      stem: "Which data structure follows LIFO principle?",
      choices: ["Queue", "Stack", "Array", "Linked List"],
      correctIndex: 1,
      answerIndex: 2,
      isCorrect: false,
      noteTitle: "Data Structures",
    }),
    conversationButton: true,
  },
  decorators: [
    () => ({
      template:
        '<div style="max-width: 800px; margin: 0 auto; padding: 20px;"><story /></div>',
    }),
  ],
}
