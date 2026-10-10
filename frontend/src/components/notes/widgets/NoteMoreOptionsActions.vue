<template>
  <template v-if="layout === 'menu'">
    <NoteMoreOptionsYieldedItems
      :only="only"
      :as-markdown="asMarkdown"
      @close-dialog="closeDialogIfMenu"
      @edit-as-markdown="emit('edit-as-markdown', $event)"
      @open-wiki="emit('open-wiki')"
      @open-new="emit('open-new')"
    />

    <DropdownMenuItem v-if="showMenuAction('export')">
      <PopButton
        ref="exportPopButtonRef"
        :btn-class="dropdownMenuButtonClass"
        :title="titles.export"
      >
        <template #button_face>
          <Upload class="shrink-0" :size="20" aria-hidden="true" />
          <span>{{ titles.export }}</span>
        </template>
        <template #default="{ closer }">
          <NoteExportForm :note="note" @close-dialog="closer" />
        </template>
      </PopButton>
    </DropdownMenuItem>

    <DropdownMenuItem v-if="showMenuAction('mcqs')">
      <PopButton :btn-class="dropdownMenuButtonClass" :title="titles.mcqs">
        <template #button_face>
          <MessageCircleQuestion class="shrink-0" :size="20" aria-hidden="true" />
          <span>{{ titles.mcqs }}</span>
        </template>
        <template #default>
          <Mcqs v-bind="{ note }" />
        </template>
      </PopButton>
    </DropdownMenuItem>

    <template v-for="action in plainActions" :key="action.id">
      <DropdownMenuItem
        v-if="showMenuAction(action.id) && action.available && !action.pressed"
      >
        <DropdownMenuActionButton
          :title="action.title"
          :icon="action.icon"
          @click="action.onClick"
        />
      </DropdownMenuItem>
    </template>
  </template>

  <template v-else>
    <PopButton
      v-if="showToolbarAction('export')"
      ref="exportPopButtonRef"
      :title="titles.export"
      :aria-label="titles.export"
    >
      <template #button_face>
        <Upload class="w-6 h-6" aria-hidden="true" />
      </template>
      <template #default="{ closer }">
        <NoteExportForm :note="note" @close-dialog="closer" />
      </template>
    </PopButton>

    <PopButton
      v-if="showToolbarAction('mcqs')"
      :title="titles.mcqs"
      :aria-label="titles.mcqs"
    >
      <template #button_face>
        <MessageCircleQuestion class="w-6 h-6" aria-hidden="true" />
      </template>
      <template #default>
        <Mcqs v-bind="{ note }" />
      </template>
    </PopButton>

    <template v-for="action in plainActions" :key="action.id">
      <NoteVoiceInputButton
        v-if="action.id === 'voiceInput'"
        v-show="showToolbarAction(action.id)"
        :key="voiceInputNoteId"
        :ref="setVoiceInputButton"
        :note="note"
      />
      <button
        v-else-if="showToolbarAction(action.id) && action.available"
        type="button"
        :class="
          action.toggleable
            ? toolbarToggleBtnClass(action.pressed)
            : toolbarGhostBtnClass
        "
        :title="action.title"
        :aria-label="action.title"
        :aria-pressed="action.toggleable ? action.pressed : undefined"
        @click="action.onClick"
      >
        <component :is="action.icon" class="w-6 h-6" aria-hidden="true" />
      </button>
    </template>
  </template>

  <RefineNoteModal
    v-if="noteHasContent && showRefineNoteModal"
    v-model:open="showRefineNoteModal"
    :note="note"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import { hasNoteContent } from "@/utils/hasNoteContent"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import Mcqs from "@/components/notes/Mcqs.vue"
import { MessageCircleQuestion, Upload } from "@lucide/vue"
import NoteExportForm from "@/components/notes/core/NoteExportForm.vue"
import RefineNoteModal from "@/components/recall/RefineNoteModal.vue"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { voiceInputIsActive } from "@/composables/useNoteVoiceInput"
import { useNoteRemovalFlow } from "@/composables/useNoteRemovalFlow"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"
import NoteMoreOptionsYieldedItems from "./NoteMoreOptionsYieldedItems.vue"
import NoteVoiceInputButton from "./NoteVoiceInputButton.vue"
import {
  noteDeleteTitle,
  noteMoreOptionsTitles,
  type NoteMoreOptionsActionId,
} from "./noteMoreOptionsTitles"
import { plainNoteActions } from "./noteMoreOptionsPlainActions"
import {
  toolbarGhostBtnClass,
  toolbarToggleBtnClass,
} from "./noteToolbarButtonClasses"
import { useKeyboardShortcut } from "@/composables/useKeyboardShortcut"
import { computed, ref, watch, type ComponentPublicInstance } from "vue"

const titles = noteMoreOptionsTitles

const props = withDefaults(
  defineProps<{
    note: Note
    layout: "toolbar" | "menu"
    omit?: NoteMoreOptionsActionId[]
    only?: NoteMoreOptionsActionId[]
    asMarkdown?: boolean
  }>(),
  { omit: () => [], asMarkdown: false }
)

const emit = defineEmits<{
  (e: "close-dialog"): void
  (e: "edit-as-markdown", value: boolean): void
  (e: "open-wiki"): void
  (e: "open-new"): void
  (e: "start-voice-input"): void
}>()

const { toggle, isOpenForNote } = useAssimilationView()
const noteId = computed(() => props.note.id)
const noteTitle = computed(() => props.note.noteTopology.title)
const { trashNote, permanentlyDeleteNote, noteIsTrashed } = useNoteRemovalFlow(
  noteId,
  noteTitle
)
const deleteTitle = computed(() => noteDeleteTitle(noteIsTrashed.value))
const deleteNote = () =>
  noteIsTrashed.value ? permanentlyDeleteNote() : trashNote()

const exportPopButtonRef = ref<InstanceType<typeof PopButton> | null>(null)

useKeyboardShortcut("note-export", () => {
  exportPopButtonRef.value?.openDialog()
})

useKeyboardShortcut("note-trash", deleteNote)

const noteHasContent = computed(() => hasNoteContent(props.note.content))
const showRefineNoteModal = ref(false)

const isAssimilationOpen = computed(() => isOpenForNote(props.note.id))
const showToolbarAction = (id: NoteMoreOptionsActionId) =>
  props.layout === "toolbar" && !props.omit.includes(id)
const showMenuAction = (id: NoteMoreOptionsActionId) =>
  !props.only || props.only.includes(id)

const closeDialogIfMenu = () => {
  if (props.layout === "menu") {
    emit("close-dialog")
  }
}

const voiceInputButton = ref<InstanceType<typeof NoteVoiceInputButton> | null>(
  null
)
const setVoiceInputButton = (
  button: Element | ComponentPublicInstance | null
) => {
  voiceInputButton.value = button as InstanceType<
    typeof NoteVoiceInputButton
  > | null
}

// An idle button follows the note on the page; a recording stays with its note.
const voiceInputNoteId = ref(props.note.id)
watch([() => props.note.id, voiceInputIsActive], ([id, active]) => {
  if (!active) voiceInputNoteId.value = id
})

const onVoiceInputStart = () => {
  emit("start-voice-input")
  closeDialogIfMenu()
}

defineExpose({ startVoiceInput: () => voiceInputButton.value?.start() })

const onAssimilationToggle = () => {
  toggle(props.note.id)
  closeDialogIfMenu()
}

const onRefineOpen = () => {
  showRefineNoteModal.value = true
  closeDialogIfMenu()
}

const plainActions = computed(() =>
  plainNoteActions({
    noteHasContent: noteHasContent.value,
    isVoiceInputActive: voiceInputIsActive.value,
    isAssimilationOpen: isAssimilationOpen.value,
    deleteTitle: deleteTitle.value,
    onRefineOpen,
    onVoiceInputStart,
    onAssimilationToggle,
    deleteNote,
  })
)
</script>
