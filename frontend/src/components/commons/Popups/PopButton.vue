<template>
  <a
    :class="`btn btn-sm ${btnClass}`"
    :aria-label="ariaLabel"
    role="button"
    @click.prevent="show=true"
    :title="title"
  >
    <slot name="button_face" />
    <template v-if="!$slots.button_face">
      {{ title }}
    </template>
  </a>
  <Modal v-if="show" :sidebar="sidebar" @close_request="closeDialog">
    <template #body>
      <slot name="default" :closer="closeDialog" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import Modal from "../Modal.vue"

defineProps({
  title: {
    type: String,
    default: "",
  },
  sidebar: String as PropType<"left" | "right">,
  btnClass: String,
  ariaLabel: String,
})

const show = ref(false)

const closeDialog = () => {
  show.value = false
}
</script>
