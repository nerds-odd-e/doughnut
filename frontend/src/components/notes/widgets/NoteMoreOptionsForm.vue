<template>
  <DropdownMenu size="wide">
    <DropdownMenuItem>
      <PopButton :btn-class="dropdownMenuButtonClass" title="Export...">
        <template #button_face>
          <Upload class="shrink-0" :size="20" aria-hidden="true" />
          <span>Export...</span>
        </template>
        <template #default="{ closer }">
          <NoteExportForm :note="note" @close-dialog="closer" />
        </template>
      </PopButton>
    </DropdownMenuItem>

    <DropdownMenuItem>
      <PopButton
        :btn-class="dropdownMenuButtonClass"
        title="Questions for the note"
      >
        <template #button_face>
          <MessageCircleQuestion class="shrink-0" :size="20" aria-hidden="true" />
          <span>Questions for the note</span>
        </template>
        <template #default>
          <Questions v-bind="{ note }" />
        </template>
      </PopButton>
    </DropdownMenuItem>

    <DropdownMenuItem>
      <DropdownMenuActionButton
        title="Assimilation settings"
        :icon="CircleCheck"
        @click="assimilateNote"
      />
    </DropdownMenuItem>

    <DropdownMenuItem>
      <DropdownMenuActionButton
        title="Delete note"
        :icon="Trash2"
        @click="deleteNote"
      />
    </DropdownMenuItem>
  </DropdownMenu>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import Questions from "@/components/notes/Questions.vue"
import { CircleCheck, MessageCircleQuestion, Trash2, Upload } from "@lucide/vue"
import NoteExportForm from "@/components/notes/core/NoteExportForm.vue"
import { useRouter } from "vue-router"
import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteDeleteReferenceHandling } from "@/store/StoredApiCollection"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "close-dialog"): void
}>()

const router = useRouter()
const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const closeDialog = () => {
  emit("close-dialog")
}

const assimilateNote = () => {
  router.push({ name: "assimilateSingleNote", params: { noteId: note.id } })
  closeDialog()
}

const noteHasReferences = () =>
  (storageAccessor.value.refOfNoteRealm(note.id).value?.references?.length ??
    0) > 0

const chooseDeleteReferenceHandling =
  async (): Promise<NoteDeleteReferenceHandling | null> => {
    if (!noteHasReferences()) {
      return (await popups.confirm(`Confirm to delete this note?`))
        ? "LEAVE_DEAD_LINKS"
        : null
    }
    return (await popups.options(
      "This note has references. How should they be handled?",
      [
        {
          label:
            "Remove from properties of references (undo will not recover the removed property)",
          value: "REMOVE_FROM_PROPERTIES",
        },
        {
          label: "Leave all references as dead link",
          value: "LEAVE_DEAD_LINKS",
        },
      ]
    )) as NoteDeleteReferenceHandling | null
  }

const deleteNote = async () => {
  const referenceHandling = await chooseDeleteReferenceHandling()
  if (!referenceHandling) return
  await storageAccessor.value
    .storedApi()
    .deleteNote(router, note.id, referenceHandling)
}
</script>
