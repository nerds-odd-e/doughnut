import type { Meta, StoryObj } from '@storybook/vue3'
import QuestionDisplay from './QuestionDisplay.vue'
import makeMe from '@tests/fixtures/makeMe'
import type { MultipleChoicesQuestion } from '@generated/backend'

const meta = {
  title: 'Review/QuestionDisplay',
  component: QuestionDisplay,
  tags: ['autodocs'],
  argTypes: {
    multipleChoicesQuestion: {
      control: 'object',
    },
    correctChoiceIndex: {
      control: 'number',
    },
    answer: {
      control: 'object',
    },
    disabled: {
      control: 'boolean',
    },
  },
} satisfies Meta<typeof QuestionDisplay>

export default meta
type Story = StoryObj<typeof meta>

// Question without answer
export const Unanswered: Story = {
  args: {
    multipleChoicesQuestion: makeMe.aPredefinedQuestion
      .withQuestionStem('What is the capital of France?')
      .withChoices(['Paris', 'London', 'Berlin', 'Madrid'])
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
      .withQuestionStem('Which programming language is used for web development?')
      .withChoices(['JavaScript', 'Python', 'Java', 'C++'])
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
      .withQuestionStem('What is 2 + 2?')
      .withChoices(['3', '4', '5', '6'])
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
      .withQuestionStem('Which data structure follows LIFO?')
      .withChoices(['Queue', 'Stack', 'Array', 'Linked List'])
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
      .withQuestionStem('Which of the following are JavaScript frameworks? Select all that apply.')
      .withChoices([
        'React',
        'Vue',
        'Angular',
        'Svelte',
        'Python',
        'Java',
        'TypeScript',
        'Node.js'
      ])
      .correctAnswerIndex(0)
      .please().multipleChoicesQuestion,
    correctChoiceIndex: 0,
    disabled: false,
  },
}
