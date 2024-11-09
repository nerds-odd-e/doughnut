<template>
  <PopButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteNewDialog
        v-bind="{ referenceNote, insertMode, storageAccessor }"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script lang="ts">
import type { PropType } from "vue"
import { defineComponent } from "vue"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import type { Note } from "@/generated/backend"
import type { InsertMode } from "@/models/InsertMode"
import PopButton from "../../commons/Popups/PopButton.vue"
import NoteNewDialog from "../NoteNewDialog.vue"

export default defineComponent({
  props: {
    referenceNote: { type: Object as PropType<Note>, required: true },
    insertMode: { type: String as PropType<InsertMode>, required: true },
    buttonTitle: { type: String, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { PopButton, NoteNewDialog },
})
</script>
