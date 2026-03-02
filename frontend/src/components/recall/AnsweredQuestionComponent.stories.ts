import type { Meta, StoryObj } from "@storybook/vue3"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import makeMe from "@tests/fixtures/makeMe"
import type { RecallPrompt } from "@generated/backend"

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

const createAnsweredQuestionWithQuestion = (
  stem: string,
  choices: string[],
  correctIndex: number,
  answerIndex: number,
  isCorrect: boolean
): RecallPrompt => {
  const predefinedQuestion = makeMe.aPredefinedQuestion
    .withQuestionStem(stem)
    .withChoices(choices)
    .correctAnswerIndex(correctIndex)
    .please()

  const base = makeMe.anAnsweredQuestion
    .answerCorrect(isCorrect)
    .withChoiceIndex(answerIndex)
    .please()

  const note = base?.note ?? makeMe.aNote.please()
  return makeMe.aRecallPrompt
    .withPredefinedQuestion(predefinedQuestion)
    .withNote(note)
    .withAnswer(
      base?.answer ?? {
        id: 1,
        correct: isCorrect,
        choiceIndex: answerIndex,
      }
    )
    .withId(base?.id ?? 1)
    .please()
}

export const CorrectAnswer: Story = {
  args: {
    answeredQuestion: (() => {
      const question = createAnsweredQuestionWithQuestion(
        "What is the capital of France?",
        ["Paris", "London", "Berlin", "Madrid"],
        0,
        0,
        true
      )
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
      const question = createAnsweredQuestionWithQuestion(
        "What is the capital of France?",
        ["Paris", "London", "Berlin", "Madrid"],
        0,
        1,
        false
      )
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
      const question = createAnsweredQuestionWithQuestion(
        "What is TypeScript?",
        ["A programming language", "A database", "A framework", "A browser"],
        0,
        0,
        true
      )

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
      const question = createAnsweredQuestionWithQuestion(
        "Which of the following is a JavaScript framework?",
        ["React", "Python", "Java", "C++"],
        0,
        0,
        true
      )
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
      const question = createAnsweredQuestionWithQuestion(
        "What is 2 + 2?",
        ["3", "4", "5", "6"],
        1,
        1,
        true
      )
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
      const question = createAnsweredQuestionWithQuestion(
        "Which data structure follows LIFO principle?",
        ["Queue", "Stack", "Array", "Linked List"],
        1,
        2,
        false
      )
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
