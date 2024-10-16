<template>
  <PopButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteSendMessageDialog
        v-bind="{ noteId }"
        @submitted="
            closer();"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script lang="ts">
import { defineComponent } from "vue"
import PopButton from "../../commons/Popups/PopButton.vue"
import NoteSendMessageDialog from "../NoteSendMessageDialog.vue"

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    buttonTitle: { type: String, required: true },
  },
  components: { PopButton, NoteSendMessageDialog },
})
</script>
