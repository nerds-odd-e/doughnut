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

    <DropdownMenuItem>
      <DropdownMenuActionButton
        :title="titles.assimilation"
        :icon="CircleCheck"
        :checked="assimilationChecked"
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

    <PopButton :title="titles.questions" :aria-label="titles.questions">
      <template #button_face>
        <MessageCircleQuestion class="w-6 h-6" aria-hidden="true" />
      </template>
      <template #default>
        <Questions v-bind="{ note }" />
      </template>
    </PopButton>

    <button
      type="button"
      :class="[
        toolbarGhostBtnClass,
        { 'daisy-btn-active': assimilationChecked },
      ]"
      :title="titles.assimilation"
      :aria-label="titles.assimilation"
      @click="onAssimilationToggle"
    >
      <CircleCheck class="w-6 h-6" aria-hidden="true" />
    </button>

    <button
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
import { CircleCheck, MessageCircleQuestion, Trash2, Upload } from "@lucide/vue"
import NoteExportForm from "@/components/notes/core/NoteExportForm.vue"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteDeleteFlow } from "@/composables/useNoteDeleteFlow"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"
import { noteMoreOptionsTitles } from "./noteMoreOptionsTitles"
import { useKeyboardShortcut } from "@/composables/useKeyboardShortcut"
import { computed, ref } from "vue"

const toolbarGhostBtnClass = "daisy-btn daisy-btn-ghost daisy-btn-sm"
const titles = noteMoreOptionsTitles

const props = defineProps<{
  note: Note
  layout: "toolbar" | "menu"
}>()

const emit = defineEmits<{
  (e: "close-dialog"): void
}>()

const { toggle, isOpenForNote } = useAssimilationView()
const noteId = computed(() => props.note.id)
const noteTitle = computed(() => props.note.noteTopology.title)
const { deleteNote } = useNoteDeleteFlow(noteId, noteTitle)

const exportPopButtonRef = ref<InstanceType<typeof PopButton> | null>(null)

useKeyboardShortcut("note-export", () => {
  exportPopButtonRef.value?.openDialog()
})

useKeyboardShortcut("note-delete", deleteNote)

const assimilationChecked = computed(() => isOpenForNote(props.note.id))

const onAssimilationToggle = () => {
  toggle(props.note.id)
  if (props.layout === "menu") {
    emit("close-dialog")
  }
}
</script>
