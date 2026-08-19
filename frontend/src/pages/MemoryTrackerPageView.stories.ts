import type { Meta, StoryObj } from "@storybook/vue3-vite"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"
import makeMe from "doughnut-test-fixtures/makeMe"

const meta = {
  title: "Page Views/MemoryTrackerPageView",
  component: MemoryTrackerPageView,
  tags: ["autodocs"],
  argTypes: {
    recallHistory: {
      control: "object",
    },
  },
} satisfies Meta<typeof MemoryTrackerPageView>

export default meta
type Story = StoryObj<typeof meta>

const createRecallPromptWithAnswer = (opts: {
  stem: string
  choices: string[]
  correctIndex: number
  answerIndex: number
  isCorrect: boolean
}) => {
  const mcq = makeMe.anMcq
    .withQuestionStem(opts.stem)
    .withChoices(opts.choices)
    .correctAnswerIndex(opts.correctIndex)
    .please()

  return makeMe.aRecallPromptHistoryItem
    .withMcq(mcq)
    .withAnswer({
      id: 1,
      correct: opts.isCorrect,
      choiceIndex: opts.answerIndex,
    })
    .withAnswerTime(new Date().toISOString())
    .please()
}

export const WithAnsweredQuestion: Story = {
  args: {
    recallHistory: [
      makeMe.aRecallHistoryItem
        .recallPrompt(
          createRecallPromptWithAnswer({
            stem: "What is the capital of France?",
            choices: ["Paris", "London", "Berlin", "Madrid"],
            correctIndex: 0,
            answerIndex: 0,
            isCorrect: true,
          })
        )
        .please(),
    ],
    memoryTracker: makeMe.aMemoryTracker.please(),
    memoryTrackerId: 1,
  },
}

export const WithIncorrectAnswer: Story = {
  args: {
    recallHistory: [
      makeMe.aRecallHistoryItem
        .recallPrompt(
          createRecallPromptWithAnswer({
            stem: "What is the capital of France?",
            choices: ["Paris", "London", "Berlin", "Madrid"],
            correctIndex: 0,
            answerIndex: 1,
            isCorrect: false,
          })
        )
        .please(),
    ],
    memoryTracker: makeMe.aMemoryTracker.please(),
    memoryTrackerId: 1,
  },
}

export const NoteWithManyAncestors: Story = {
  args: (() => {
    const note = makeMe.aNote
      .title("TypeScript")
      .content(
        "TypeScript is a typed superset of JavaScript that compiles to plain JavaScript."
      )
      .please()

    const mcq = makeMe.anMcq
      .withQuestionStem("What is TypeScript?")
      .withChoices([
        "A programming language",
        "A database",
        "A framework",
        "A browser",
      ])
      .correctAnswerIndex(0)
      .please()

    return {
      recallHistory: [
        makeMe.aRecallHistoryItem
          .recallPrompt(
            makeMe.aRecallPromptHistoryItem
              .withMcq(mcq)
              .withAnswer({
                id: 1,
                correct: true,
                choiceIndex: 0,
              })
              .withAnswerTime(new Date().toISOString())
              .please()
          )
          .please(),
      ],
      memoryTracker: makeMe.aMemoryTracker.ofLink(note).please(),
      memoryTrackerId: 1,
    }
  })(),
}

export const NoHistoryFound: Story = {
  args: {
    recallHistory: [],
    memoryTracker: makeMe.aMemoryTracker.please(),
    memoryTrackerId: 1,
  },
}
