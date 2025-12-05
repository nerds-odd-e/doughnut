import type { Meta, StoryObj } from "@storybook/vue3"
import QuestionDisplay from "./QuestionDisplay.vue"
import makeMe from "@tests/fixtures/makeMe"

const meta = {
  title: "Recall/QuestionDisplay",
  component: QuestionDisplay,
  tags: ["autodocs"],
  parameters: {
    layout: "fullscreen",
  },
  decorators: [
    () => ({
      template:
        '<div style="width: 100vw; padding: 2rem; box-sizing: border-box;"><story /></div>',
    }),
  ],
  argTypes: {
    multipleChoicesQuestion: {
      control: "object",
    },
    correctChoiceIndex: {
      control: "number",
    },
    answer: {
      control: "object",
    },
    disabled: {
      control: "boolean",
    },
  },
} satisfies Meta<typeof QuestionDisplay>

export default meta
type Story = StoryObj<typeof meta>

// Question without answer
export const Unanswered: Story = {
  args: {
    multipleChoicesQuestion: makeMe.aPredefinedQuestion
      .withQuestionStem("What is the capital of France?")
      .withChoices(["Paris", "London", "Berlin", "Madrid"])
      .correctAnswerIndex(0)
      .please().multipleChoicesQuestion,
    correctChoiceIndex: 0,
    disabled: false,
  },
}

// Question with correct answer selected
export const WithCorrectAnswer: Story = {
  args: {
    multipleChoicesQuestion: makeMe.aPredefinedQuestion
      .withQuestionStem(
        "Which programming language is used for web development?"
      )
      .withChoices(["JavaScript", "Python", "Java", "C++"])
      .correctAnswerIndex(0)
      .please().multipleChoicesQuestion,
    correctChoiceIndex: 0,
    answer: {
      id: 1,
      correct: true,
      choiceIndex: 0,
    },
    disabled: true,
  },
}

// Question with incorrect answer selected
export const WithIncorrectAnswer: Story = {
  args: {
    multipleChoicesQuestion: makeMe.aPredefinedQuestion
      .withQuestionStem("What is 2 + 2?")
      .withChoices(["3", "4", "5", "6"])
      .correctAnswerIndex(1)
      .please().multipleChoicesQuestion,
    correctChoiceIndex: 1,
    answer: {
      id: 1,
      correct: false,
      choiceIndex: 0,
    },
    disabled: true,
  },
}

// Question disabled (no interaction)
export const Disabled: Story = {
  args: {
    multipleChoicesQuestion: makeMe.aPredefinedQuestion
      .withQuestionStem("Which data structure follows LIFO?")
      .withChoices(["Queue", "Stack", "Array", "Linked List"])
      .correctAnswerIndex(1)
      .please().multipleChoicesQuestion,
    correctChoiceIndex: 1,
    disabled: true,
  },
}

// Long question with many choices
export const LongQuestion: Story = {
  args: {
    multipleChoicesQuestion: makeMe.aPredefinedQuestion
      .withQuestionStem(
        "Which of the following are JavaScript frameworks? Select all that apply."
      )
      .withChoices([
        "React",
        "Vue",
        "Angular",
        "Svelte",
        "Python",
        "Java",
        "TypeScript",
        "Node.js",
      ])
      .correctAnswerIndex(0)
      .please().multipleChoicesQuestion,
    correctChoiceIndex: 0,
    disabled: false,
  },
}

// Question with long choice text
export const LongChoiceText: Story = {
  args: {
    multipleChoicesQuestion: makeMe.aPredefinedQuestion
      .withQuestionStem(
        "Which of the following best describes the purpose of a RESTful API?"
      )
      .withChoices([
        "A RESTful API is an architectural style for designing networked applications that uses stateless communication and standard HTTP methods to interact with resources, allowing clients to access and manipulate web resources through a uniform interface.",
        "A RESTful API is a type of database that stores data in a structured format using tables and relationships between them.",
        "A RESTful API is a programming language used for building web applications with a focus on object-oriented design patterns.",
        "A RESTful API is a framework for building user interfaces that allows developers to create interactive web pages using component-based architecture.",
      ])
      .correctAnswerIndex(0)
      .please().multipleChoicesQuestion,
    correctChoiceIndex: 0,
    disabled: false,
  },
}
