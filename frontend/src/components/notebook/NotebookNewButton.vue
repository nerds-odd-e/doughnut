<template>
  <PopButton class="btn-primary" title="New notebook">
    <template #button_face>
      <slot />
    </template>
    <NotebookNewDialog :circle="circle" />
  </PopButton>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NotebookNewDialog from "./NotebookNewDialog.vue";
import PopButton from "../commons/Popups/PopButton.vue";

export default defineComponent({
  props: { circle: Object },
  components: { PopButton, NotebookNewDialog },
});
</script>
