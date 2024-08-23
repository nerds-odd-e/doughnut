<template>
  <a
    :class="`btn btn-sm ${btnClass} ${disabled ? 'disabled' : ''}`"
    :aria-label="ariaLabel"
    role="button"
    @click.prevent="handleClick"
    :title="computedTitle"
    :style="{ pointerEvents: disabled ? 'none' : 'auto', opacity: disabled ? 0.6 : 1 }"
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

<script lang="ts">
import { PropType, defineComponent } from "vue"
import Modal from "../Modal.vue"

export default defineComponent({
  props: {
    title: {
      type: String,
      default: "",
    },
    disabledTitle: {
      type: String,
      default: "",
    },
    disabled: Boolean,
    sidebar: String as PropType<"left" | "right">,
    btnClass: String,
    ariaLabel: String,
  },
  data() {
    return { show: false }
  },
  components: { Modal },
  methods: {
    closeDialog() {
      this.show = false
    },
    handleClick() {
      if (!this.disabled) {
        this.show = true
      }
    },
  },
  computed: {
    computedTitle(): string {
      return this.disabled ? this.disabledTitle : this.title
    },
  },
})
</script>
