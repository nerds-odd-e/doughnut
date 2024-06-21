<template>
  <PopButton btn-class="btn-primary" title="New notebook">
    <template #button_face>
      <slot />
    </template>
    <NotebookNewDialog :circle="circle" />
  </PopButton>
</template>

<script lang="ts">
import { Circle } from "@/generated/backend"
import { PropType, defineComponent } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import NotebookNewDialog from "./NotebookNewDialog.vue"

export default defineComponent({
  props: { circle: { type: Object as PropType<Circle> } },
  components: { PopButton, NotebookNewDialog },
})
</script>
