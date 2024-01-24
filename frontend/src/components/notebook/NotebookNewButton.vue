<template>
  <PopButton btn-class="btn-primary" title="New notebook">
    <template #button_face>
      <slot />
    </template>
    <NotebookNewDialog :circle="circle" />
  </PopButton>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import NotebookNewDialog from "./NotebookNewDialog.vue";
import PopButton from "../commons/Popups/PopButton.vue";

export default defineComponent({
  props: { circle: { type: Object as PropType<Generated.Circle> } },
  components: { PopButton, NotebookNewDialog },
});
</script>
