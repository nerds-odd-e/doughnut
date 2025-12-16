import type { Meta, StoryObj } from "@storybook/vue3"
import AssimilationPageView from "./AssimilationPageView.vue"
import makeMe from "@tests/fixtures/makeMe"

const meta = {
  title: "Page Views/AssimilationPageView",
  component: AssimilationPageView,
  tags: ["autodocs"],
  parameters: {
    test: {
      disable: true,
    },
  },
  argTypes: {
    notes: {
      control: "object",
    },
    assimilatedCountOfTheDay: {
      control: "number",
    },
    totalUnassimilatedCount: {
      control: "number",
    },
  },
} satisfies Meta<typeof AssimilationPageView>

export default meta
type Story = StoryObj<typeof meta>

// With notes to assimilate
export const WithNotes: Story = {
  args: {
    notes: [
      makeMe.aNote
        .title("TypeScript")
        .details("TypeScript is a typed superset of JavaScript.")
        .please(),
      makeMe.aNote
        .title("Vue.js")
        .details("Vue.js is a progressive JavaScript framework.")
        .please(),
    ],
    assimilatedCountOfTheDay: 3,
    totalUnassimilatedCount: 15,
  },
}

// With single note
export const WithSingleNote: Story = {
  args: {
    notes: [
      makeMe.aNote
        .title("React")
        .details("React is a JavaScript library for building user interfaces.")
        .please(),
    ],
    assimilatedCountOfTheDay: 5,
    totalUnassimilatedCount: 10,
  },
}

// Empty state - all notes assimilated
export const Empty: Story = {
  args: {
    notes: [],
    assimilatedCountOfTheDay: 10,
    totalUnassimilatedCount: 0,
  },
}

// Loading state
export const Loading: Story = {
  args: {
    notes: undefined,
    assimilatedCountOfTheDay: 0,
    totalUnassimilatedCount: undefined,
  },
}

// With high progress
export const HighProgress: Story = {
  args: {
    notes: [
      makeMe.aNote
        .title("Python")
        .details("Python is a high-level programming language.")
        .please(),
    ],
    assimilatedCountOfTheDay: 8,
    totalUnassimilatedCount: 2,
  },
}

// With no total count (only daily progress shown)
export const DailyProgressOnly: Story = {
  args: {
    notes: [
      makeMe.aNote
        .title("JavaScript")
        .details("JavaScript is a programming language.")
        .please(),
    ],
    assimilatedCountOfTheDay: 5,
    totalUnassimilatedCount: undefined,
  },
}
