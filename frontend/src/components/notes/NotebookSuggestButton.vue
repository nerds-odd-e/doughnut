<template>
  <PopupButton class="btn-primary" title="Suggest">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{ doneHandler }">
      <NotebookSuggestDialog @done="doneHandler($event)" />
    </template>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NotebookSuggestDialog from "./NotebookSuggestDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
  components: { PopupButton, NotebookSuggestDialog },
});
</script>
