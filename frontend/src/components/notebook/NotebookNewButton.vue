<template>
  <PopupButton class="btn-primary" title="New notebook">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{ doneHandler }">
      <NotebookNewDialog :circle="circle" @done="doneHandler($event)" />
    </template>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NotebookNewDialog from "./NotebookNewDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
  props: { circle: Object },
  components: { PopupButton, NotebookNewDialog },
});
</script>
