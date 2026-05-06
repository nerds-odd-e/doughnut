import type { Meta, StoryObj } from "@storybook/vue3-vite"
import NoteMoreOptionsDialog from "./NoteMoreOptionsDialog.vue"
import makeMe from "doughnut-test-fixtures/makeMe"

const meta = {
  title: "Notes/Accessory/NoteMoreOptionsDialog",
  component: NoteMoreOptionsDialog,
  tags: ["autodocs"],
  parameters: {
    layout: "padded",
    test: {
      disable: true,
    },
  },
  decorators: [
    (story) => ({
      components: { story },
      template:
        '<div style="width: 100%; max-width: 600px; background: hsl(var(--b1)); padding: 1rem;"><story /></div>',
    }),
  ],
  argTypes: {
    note: {
      control: "object",
      description: "The note for which to show more options",
    },
  },
} satisfies Meta<typeof NoteMoreOptionsDialog>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    note: makeMe.aNoteRealm.title("Sample Note").please().note,
  },
}

export const WithLongTitle: Story = {
  args: {
    note: makeMe.aNoteRealm
      .title("This is a very long note title that might wrap to multiple lines")
      .please().note,
  },
}

export const WithDetails: Story = {
  args: {
    note: makeMe.aNoteRealm
      .title("Note with Details")
      .details("This note has some details content")
      .please().note,
  },
}

export const WithParent: Story = {
  args: {
    note: makeMe.aNoteRealm
      .title("Child Note")
      .under(makeMe.aNoteRealm.title("Parent Note").please())
      .please().note,
  },
}
