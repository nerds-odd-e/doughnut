import type { Meta, StoryObj } from "@storybook/vue3-vite"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import { testFolderStub } from "@tests/helpers"

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
      testFolderStub(1, "Programming"),
      testFolderStub(2, "Languages"),
    ],
  },
}

// Long folder trail (testing horizontal scroll)
export const WithManyAncestors: Story = {
  args: {
    noteTopology: makeMe.aNote.title("TypeScript").please().noteTopology,
    ancestorFolders: Array.from({ length: 10 }, (_, i) =>
      testFolderStub(i + 1, `Ancestor ${i + 1}`)
    ),
  },
  decorators: [
    () => ({
      template:
        '<div style="max-width: 600px; margin: 0 auto; padding: 20px;"><story /></div>',
    }),
  ],
}
