import type { Meta, StoryObj } from "@storybook/vue3"
import NoteMoreOptionsDialog from "./NoteMoreOptionsDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import { NoteController } from "@generated/backend/sdk.gen"
import type { NoteInfo } from "@generated/backend"
import type { Options } from "@generated/backend/client/types.gen"
import type { GetNoteInfoData } from "@generated/backend"

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
    (story) => {
      // Mock NoteController.getNoteInfo to avoid API calls
      const originalGetNoteInfo = NoteController.getNoteInfo
      const mockNoteInfo: NoteInfo = {
        note: makeMe.aNoteRealm.topicConstructor("Sample Note").please(),
        recallSetting: {
          level: 0,
          rememberSpelling: false,
          skipMemoryTracking: false,
        },
        memoryTrackers: [],
        createdAt: "",
      }
      NoteController.getNoteInfo = (async <
        ThrowOnError extends boolean = false,
      >(
        _options: Options<GetNoteInfoData, ThrowOnError>
      ) => {
        return {
          data: mockNoteInfo,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        }
      }) as typeof NoteController.getNoteInfo

      return {
        components: { story },
        template:
          '<div style="width: 100%; max-width: 600px; background: hsl(var(--b1)); padding: 1rem;"><story /></div>',
        beforeUnmount() {
          // Restore original method when story unmounts
          NoteController.getNoteInfo = originalGetNoteInfo
        },
      }
    },
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
    note: makeMe.aNoteRealm.topicConstructor("Sample Note").please().note,
  },
}

export const WithLongTitle: Story = {
  args: {
    note: makeMe.aNoteRealm
      .topicConstructor(
        "This is a very long note title that might wrap to multiple lines"
      )
      .please().note,
  },
}

export const WithDetails: Story = {
  args: {
    note: makeMe.aNoteRealm
      .topicConstructor("Note with Details")
      .details("This note has some details content")
      .please().note,
  },
}

export const WithParent: Story = {
  args: {
    note: makeMe.aNoteRealm
      .topicConstructor("Child Note")
      .under(makeMe.aNoteRealm.topicConstructor("Parent Note").please())
      .please().note,
  },
}
