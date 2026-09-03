import type { Meta, StoryObj } from "@storybook/vue3-vite"
import type { Note } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { computed, defineComponent, provide, ref, type PropType } from "vue"
import {
  memoryTrackerActionsKey,
  type MemoryTrackerActions,
} from "@/composables/useMemoryTrackerActions"
import AssimilationPanel from "./AssimilationPanel.vue"

const idleActionResult = { completed: false, navigated: false }

const AssimilationPanelStory = defineComponent({
  components: { AssimilationPanel },
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  setup() {
    const memoryTrackerActions: MemoryTrackerActions = {
      noteInfoLoaded: ref(true),
      noteRecallInfo: ref(makeMe.aNoteRecallInfo.please()),
      assimilatingPropertyKey: ref(null),
      showSpellingPopup: computed(() => false),
      reloadNoteInfo: async () => undefined,
      assimilate: async () => idleActionResult,
      skip: async () => idleActionResult,
      returnToSequence: async () => idleActionResult,
      handleSpellingVerified: async () => idleActionResult,
      handleSpellingCancel: () => undefined,
    }

    provide(memoryTrackerActionsKey, memoryTrackerActions)
  },
  template: '<AssimilationPanel :note="note" />',
})

const meta = {
  title: "Recall/AssimilationPanel",
  component: AssimilationPanelStory,
  tags: ["autodocs"],
  parameters: {
    layout: "centered",
  },
} satisfies Meta<typeof AssimilationPanelStory>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    note: makeMe.aNote.title("Photosynthesis").please(),
  },
}
