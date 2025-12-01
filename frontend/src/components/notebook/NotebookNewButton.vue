<template>
  <PopButton title="New notebook">
    <template #button_face>
      <SvgNewNotebook />
      <slot />
    </template>
    <NotebookNewDialog :circle="circle" />
  </PopButton>
</template>

<script lang="ts">
import type { Circle } from "@generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import NotebookNewDialog from "./NotebookNewDialog.vue"
import SvgNewNotebook from "../svgs/SvgNewNotebook.vue"

export default defineComponent({
  props: { circle: { type: Object as PropType<Circle> } },
  components: { PopButton, NotebookNewDialog, SvgNewNotebook },
})
</script>
