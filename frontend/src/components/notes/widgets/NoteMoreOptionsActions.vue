<template>
  <template v-if="layout === 'menu'">
    <NoteMoreOptionsYieldedItems
      :note="note"
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

    <DropdownMenuItem v-if="showMenuAction('questions')">
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

    <DropdownMenuItem v-if="showMenuAction('audio') && !isAudioOpen">
      <DropdownMenuActionButton
        :title="titles.audio"
        :icon="Mic"
        @click="onAudioToggle"
      />
    </DropdownMenuItem>

    <DropdownMenuItem v-if="showMenuAction('assimilation') && !isAssimilationOpen">
      <DropdownMenuActionButton
        :title="titles.assimilation"
        :icon="CircleCheck"
        @click="onAssimilationToggle"
      />
    </DropdownMenuItem>

    <DropdownMenuItem v-if="showMenuAction('delete')">
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
      v-if="showToolbarAction('audio')"
      type="button"
      :class="[
        toolbarToggleBtnClass(isAudioOpen),
        { 'shrink-0': isAudioOpen },
      ]"
      :title="titles.audio"
      :aria-label="titles.audio"
      :aria-pressed="isAudioOpen"
      @click="onAudioToggle"
    >
      <Mic class="w-6 h-6" aria-hidden="true" />
    </button>

    <button
      v-if="showToolbarAction('assimilation')"
      type="button"
      :class="[
        toolbarToggleBtnClass(isAssimilationOpen),
        { 'shrink-0': isAssimilationOpen },
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
import type { Note } from "@generated/donut-backend-api"
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
import NoteMoreOptionsYieldedItems from "./NoteMoreOptionsYieldedItems.vue"
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
}>()

const { toggle, isOpenForNote } = useAssimilationView()
const { isAudioOpen, toggleAudio } = useNoteToolbarPanel()
const noteId = computed(() => props.note.id)
const noteTitle = computed(() => props.note.noteTopology.title)
const { deleteNote } = useNoteDeleteFlow(noteId, noteTitle)

const exportPopButtonRef = ref<InstanceType<typeof PopButton> | null>(null)
const shortcutScope = useNoteShortcutScope()
const shortcutsEnabled = () => shortcutScope.value

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
const showMenuAction = (id: NoteMoreOptionsActionId) =>
  !props.only || props.only.includes(id)

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
