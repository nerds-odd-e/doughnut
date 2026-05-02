import type { Meta, StoryObj } from "@storybook/vue3-vite"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import makeMe from "doughnut-test-fixtures/makeMe"

const meta = {
  title: "Recall/NoteUnderQuestion",
  component: NoteUnderQuestion,
  tags: ["autodocs"],
  argTypes: {
    noteTopology: {
      control: "object",
    },
  },
} satisfies Meta<typeof NoteUnderQuestion>

export default meta
type Story = StoryObj<typeof meta>

// Simple note with no ancestors
export const Simple: Story = {
  args: {
    noteTopology: makeMe.aNote.title("TypeScript").please().noteTopology,
  },
}

// Note with a few folder segments (breadcrumb path)
export const WithAncestors: Story = {
  args: {
    noteTopology: makeMe.aNote.title("TypeScript").please().noteTopology,
    ancestorFolders: [
      { id: "1", name: "Programming" },
      { id: "2", name: "Languages" },
    ],
  },
}

// Long folder trail (testing horizontal scroll)
export const WithManyAncestors: Story = {
  args: {
    noteTopology: makeMe.aNote.title("TypeScript").please().noteTopology,
    ancestorFolders: Array.from({ length: 10 }, (_, i) => ({
      id: String(i + 1),
      name: `Ancestor ${i + 1}`,
    })),
  },
  decorators: [
    () => ({
      template:
        '<div style="max-width: 600px; margin: 0 auto; padding: 20px;"><story /></div>',
    }),
  ],
}
