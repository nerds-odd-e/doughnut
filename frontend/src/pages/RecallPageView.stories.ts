import type { Meta, StoryObj } from "@storybook/vue3"
import RecallPageView from "./RecallPageView.vue"
import type { MemoryTrackerLite } from "@generated/backend"
import createNoteStorage from "@/store/createNoteStorage"

const meta = {
  title: "Page Views/RecallPageView",
  component: RecallPageView,
  tags: ["autodocs"],
  argTypes: {
    toRepeat: {
      control: "object",
    },
    currentIndex: {
      control: "number",
    },
    totalAssimilatedCount: {
      control: "number",
    },
    eagerFetchCount: {
      control: "number",
    },
  },
} satisfies Meta<typeof RecallPageView>

export default meta
type Story = StoryObj<typeof meta>

const createMemoryTrackerLite = (
  id: number,
  spelling = false
): MemoryTrackerLite => ({
  memoryTrackerId: id,
  spelling,
})

const mockStorageAccessor = createNoteStorage()

// Page with memory trackers to repeat
export const WithMemoryTrackers: Story = {
  args: {
    toRepeat: [
      createMemoryTrackerLite(1),
      createMemoryTrackerLite(2),
      createMemoryTrackerLite(3),
    ],
    currentIndex: 0,
    totalAssimilatedCount: 100,
    eagerFetchCount: 5,
    storageAccessor: mockStorageAccessor,
  },
}

// Page with spelling questions
export const WithSpellingQuestions: Story = {
  args: {
    toRepeat: [
      createMemoryTrackerLite(1, true),
      createMemoryTrackerLite(2, true),
    ],
    currentIndex: 0,
    totalAssimilatedCount: 50,
    eagerFetchCount: 5,
    storageAccessor: mockStorageAccessor,
  },
}

// Page in progress - some questions answered
export const InProgress: Story = {
  args: {
    toRepeat: [
      createMemoryTrackerLite(1),
      createMemoryTrackerLite(2),
      createMemoryTrackerLite(3),
      createMemoryTrackerLite(4),
      createMemoryTrackerLite(5),
    ],
    currentIndex: 2,
    totalAssimilatedCount: 100,
    eagerFetchCount: 5,
    storageAccessor: mockStorageAccessor,
  },
}

// Page with all questions finished
export const AllFinished: Story = {
  args: {
    toRepeat: [
      createMemoryTrackerLite(1),
      createMemoryTrackerLite(2),
      createMemoryTrackerLite(3),
    ],
    currentIndex: 3,
    totalAssimilatedCount: 100,
    eagerFetchCount: 5,
    storageAccessor: mockStorageAccessor,
  },
}

// Empty state - no memory trackers
export const Empty: Story = {
  args: {
    toRepeat: [],
    currentIndex: 0,
    totalAssimilatedCount: 0,
    eagerFetchCount: 5,
    storageAccessor: mockStorageAccessor,
  },
}
