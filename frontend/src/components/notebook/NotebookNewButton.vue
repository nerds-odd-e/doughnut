<template>
  <PopButton title="New notebook" :btn-class="btnClass">
    <template #button_face>
      <BookPlus class="w-6 h-6" />
      <slot />
    </template>
    <NotebookNewForm :circle="circle" />
  </PopButton>
</template>

<script lang="ts">
import type { Circle } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import NotebookNewForm from "./NotebookNewForm.vue"
import { BookPlus } from "@lucide/vue"

export default defineComponent({
  props: {
    circle: { type: Object as PropType<Circle> },
    btnClass: String,
  },
  components: { BookPlus, NotebookNewForm, PopButton },
})
</script>
