<template>
  <PopButton title="New notebook">
    <template #button_face>
      <BookPlus class="w-5 h-5" />
      <slot />
    </template>
    <NotebookNewDialog :circle="circle" />
  </PopButton>
</template>

<script lang="ts">
import type { Circle } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import NotebookNewDialog from "./NotebookNewDialog.vue"
import { BookPlus } from "lucide-vue-next"

export default defineComponent({
  props: { circle: { type: Object as PropType<Circle> } },
  components: { BookPlus, NotebookNewDialog, PopButton },
})
</script>
