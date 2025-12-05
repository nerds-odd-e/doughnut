import type { Meta, StoryObj } from "@storybook/vue3"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import makeMe from "@tests/fixtures/makeMe"

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
    noteTopology: makeMe.aNote.topicConstructor("TypeScript").please()
      .noteTopology,
  },
}

// Note with a few ancestors
export const WithAncestors: Story = {
  args: {
    noteTopology: (() => {
      let currentNote = makeMe.aNote.topicConstructor("Programming").please()
      currentNote = makeMe.aNote
        .topicConstructor("Languages")
        .underNote(currentNote)
        .please()
      currentNote = makeMe.aNote
        .topicConstructor("TypeScript")
        .underNote(currentNote)
        .please()
      return currentNote.noteTopology
    })(),
  },
}

// Note with many ancestors (testing horizontal scroll)
export const WithManyAncestors: Story = {
  args: {
    noteTopology: (() => {
      // Create a chain of 10 ancestor notes
      let currentNote = makeMe.aNote.topicConstructor("Ancestor 1").please()
      for (let i = 2; i <= 10; i++) {
        currentNote = makeMe.aNote
          .topicConstructor(`Ancestor ${i}`)
          .underNote(currentNote)
          .please()
      }
      // Create the final note with all ancestors
      currentNote = makeMe.aNote
        .topicConstructor("TypeScript")
        .underNote(currentNote)
        .please()
      return currentNote.noteTopology
    })(),
  },
  decorators: [
    () => ({
      template:
        '<div style="max-width: 600px; margin: 0 auto; padding: 20px;"><story /></div>',
    }),
  ],
}
