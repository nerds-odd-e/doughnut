<template>
  <PopButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteSendMessageDialogue
        v-bind="{ parentId, storageAccessor }"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script lang="ts">
import type { PropType } from "vue"
import { defineComponent } from "vue"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import PopButton from "../../commons/Popups/PopButton.vue"
import NoteSendMessageDialog from "../NoteSendMessageDialog.vue"

export default defineComponent({
  props: {
    parentId: { type: Number, required: true },
    buttonTitle: { type: String, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { PopButton, NoteSendMessageDialog },
})
</script>
