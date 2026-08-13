<template>
  <template v-if="layout === 'menu'">
    <DropdownMenuItem>
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

    <DropdownMenuItem>
      <PopButton :btn-class="dropdownMenuButtonClass" :title="titles.questions">
        <template #button_face>
          <MessageCircleQuestion class="shrink-0" :size="20" aria-hidden="true" />
          <span>{{ titles.questions }}</span>
        </template>
        <template #default>
          <Questions v-bind="{ note }" />
        </template>
      </PopButton>
    </DropdownMenuItem>

    <DropdownMenuItem v-if="!isAudioOpen">
      <DropdownMenuActionButton
        :title="titles.audio"
        :icon="Mic"
        @click="onAudioToggle"
      />
    </DropdownMenuItem>

    <DropdownMenuItem v-if="!isAssimilationOpen">
      <DropdownMenuActionButton
        :title="titles.assimilation"
        :icon="CircleCheck"
        @click="onAssimilationToggle"
      />
    </DropdownMenuItem>

    <DropdownMenuItem>
      <DropdownMenuActionButton
        :title="titles.delete"
        :icon="Trash2"
        @click="deleteNote"
      />
    </DropdownMenuItem>
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
      v-if="showToolbarAction('questions')"
      :title="titles.questions"
      :aria-label="titles.questions"
    >
      <template #button_face>
        <MessageCircleQuestion class="w-6 h-6" aria-hidden="true" />
      </template>
      <template #default>
        <Questions v-bind="{ note }" />
      </template>
    </PopButton>

    <button
      v-if="showToolbarToggle('audio', isAudioOpen)"
      type="button"
      :class="[
        toolbarToggleBtnClass(isAudioOpen),
        { 'shrink-0': layout === 'pinned' },
      ]"
      :title="titles.audio"
      :aria-label="titles.audio"
      :aria-pressed="isAudioOpen"
      @click="onAudioToggle"
    >
      <Mic class="w-6 h-6" aria-hidden="true" />
    </button>

    <button
      v-if="showToolbarToggle('assimilation', isAssimilationOpen)"
      type="button"
      :class="[
        toolbarToggleBtnClass(isAssimilationOpen),
        { 'shrink-0': layout === 'pinned' },
      ]"
      :title="titles.assimilation"
      :aria-label="titles.assimilation"
      :aria-pressed="isAssimilationOpen"
      @click="onAssimilationToggle"
    >
      <CircleCheck class="w-6 h-6" aria-hidden="true" />
    </button>

    <button
      v-if="showToolbarAction('delete')"
      type="button"
      :class="toolbarGhostBtnClass"
      :title="titles.delete"
      :aria-label="titles.delete"
      @click="deleteNote"
    >
      <Trash2 class="w-6 h-6" aria-hidden="true" />
    </button>
  </template>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import Questions from "@/components/notes/Questions.vue"
import {
  CircleCheck,
  MessageCircleQuestion,
  Mic,
  Trash2,
  Upload,
} from "@lucide/vue"
import NoteExportForm from "@/components/notes/core/NoteExportForm.vue"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteToolbarPanel } from "@/composables/useNoteToolbarPanel"
import { useNoteDeleteFlow } from "@/composables/useNoteDeleteFlow"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"
import {
  noteMoreOptionsTitles,
  type NoteMoreOptionsActionId,
} from "./noteMoreOptionsTitles"
import { useKeyboardShortcut } from "@/composables/useKeyboardShortcut"
import { useNoteShortcutScope } from "@/composables/noteShortcutScope"
import { computed, ref } from "vue"

const toolbarGhostBtnClass = "daisy-btn daisy-btn-ghost daisy-btn-sm"
const toolbarToggleOnBtnClass =
  "daisy-btn daisy-btn-sm daisy-btn-soft daisy-btn-primary"
const toolbarToggleBtnClass = (pressed: boolean) =>
  pressed ? toolbarToggleOnBtnClass : toolbarGhostBtnClass
const titles = noteMoreOptionsTitles

const props = withDefaults(
  defineProps<{
    note: Note
    layout: "toolbar" | "menu" | "pinned"
    omit?: NoteMoreOptionsActionId[]
  }>(),
  { omit: () => [] }
)

const emit = defineEmits<{
  (e: "close-dialog"): void
}>()

const { toggle, isOpenForNote } = useAssimilationView()
const { isAudioOpen, toggleAudio } = useNoteToolbarPanel()
const noteId = computed(() => props.note.id)
const noteTitle = computed(() => props.note.noteTopology.title)
const { deleteNote } = useNoteDeleteFlow(noteId, noteTitle)

const exportPopButtonRef = ref<InstanceType<typeof PopButton> | null>(null)
const shortcutScope = useNoteShortcutScope()
const shortcutsEnabled = () => shortcutScope.value && props.layout !== "pinned"

useKeyboardShortcut(
  "note-export",
  () => {
    exportPopButtonRef.value?.openDialog()
  },
  shortcutsEnabled
)

useKeyboardShortcut("note-delete", deleteNote, shortcutsEnabled)

const isAssimilationOpen = computed(() => isOpenForNote(props.note.id))
const showToolbarAction = (id: NoteMoreOptionsActionId) =>
  props.layout === "toolbar" && !props.omit.includes(id)
const showToolbarToggle = (id: NoteMoreOptionsActionId, isOn: boolean) =>
  showToolbarAction(id) || (props.layout === "pinned" && isOn)

const closeDialogIfMenu = () => {
  if (props.layout === "menu") {
    emit("close-dialog")
  }
}

const onAudioToggle = () => {
  toggleAudio()
  closeDialogIfMenu()
}

const onAssimilationToggle = () => {
  toggle(props.note.id)
  closeDialogIfMenu()
}
</script>
