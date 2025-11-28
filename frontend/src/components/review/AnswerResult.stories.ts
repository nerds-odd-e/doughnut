import type { Meta, StoryObj } from "@storybook/vue3"
import AnswerResult from "./AnswerResult.vue"
import makeMe from "@tests/fixtures/makeMe"

const meta = {
  title: "Review/AnswerResult",
  component: AnswerResult,
  tags: ["autodocs"],
  argTypes: {
    answeredQuestion: {
      control: "object",
    },
  },
} satisfies Meta<typeof AnswerResult>

export default meta
type Story = StoryObj<typeof meta>

// Correct answer
export const Correct: Story = {
  args: {
    answeredQuestion: makeMe.anAnsweredQuestion.answerCorrect(true).please(),
  },
}

// Incorrect answer
export const Incorrect: Story = {
  args: {
    answeredQuestion: makeMe.anAnsweredQuestion
      .answerCorrect(false)
      .withChoiceIndex(1)
      .please(),
  },
}

// Incorrect answer with custom display text
export const IncorrectWithDisplay: Story = {
  args: {
    answeredQuestion: (() => {
      const question = makeMe.anAnsweredQuestion
        .answerCorrect(false)
        .withChoiceIndex(2)
        .please()
      question.answerDisplay = "Wrong Answer"
      return question
    })(),
  },
}
