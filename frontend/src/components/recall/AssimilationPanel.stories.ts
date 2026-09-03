import type { Meta, StoryObj } from "@storybook/vue3-vite"
import type { Note, NoteRecallInfo } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import {
  computed,
  defineComponent,
  provide,
  ref,
  toRef,
  type PropType,
} from "vue"
import {
  memoryTrackerActionsKey,
  type MemoryTrackerActions,
} from "@/composables/useMemoryTrackerActions"
import NoteToolbarPanelShell from "@/components/notes/core/NoteToolbarPanelShell.vue"
import AssimilationPanel from "./AssimilationPanel.vue"

const idleActionResult = { completed: false, navigated: false }

const AssimilationPanelStory = defineComponent({
  components: { AssimilationPanel, NoteToolbarPanelShell },
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
    noteRecallInfo: {
      type: Object as PropType<NoteRecallInfo>,
      required: true,
    },
  },
  setup(props) {
    const memoryTrackerActions: MemoryTrackerActions = {
      noteInfoLoaded: ref(true),
      noteRecallInfo: toRef(props, "noteRecallInfo"),
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
  template: `
    <div class="w-[calc(100vw-2rem)] max-w-2xl">
      <NoteToolbarPanelShell>
        <AssimilationPanel :note="note" />
      </NoteToolbarPanelShell>
    </div>
  `,
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

const noteRealm = makeMe.aNoteRealm.title("Photosynthesis").please()

export const Default: Story = {
  args: {
    note: noteRealm.note,
    noteRecallInfo: makeMe.aNoteRecallInfo.please(),
  },
}

export const SpellingAlreadyAssimilated: Story = {
  args: {
    note: noteRealm.note,
    noteRecallInfo: makeMe.aNoteRecallInfo
      .memoryTrackers([
        makeMe.aMemoryTracker
          .id(42)
          .ofNote(noteRealm)
          .spelling()
          .nextRecallAt("2026-09-10T08:00:00Z")
          .recallCount(2)
          .please(),
      ])
      .please(),
  },
}
