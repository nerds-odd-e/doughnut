<template>
  <PopButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteNewDialog
        v-bind="{ parentId, storageAccessor }"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue"
import { StorageAccessor } from "../../../store/createNoteStorage"
import PopButton from "../../commons/Popups/PopButton.vue"
import NoteNewDialog from "../NoteNewDialog.vue"

export default defineComponent({
  props: {
    parentId: { type: Number, required: true },
    buttonTitle: { type: String, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { PopButton, NoteNewDialog },
})
</script>
