<template>
  <a
    :class="`btn btn-sm ${btnClass}`"
    :aria-label="ariaLabel"
    role="button"
    @click="show = true"
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

<script lang="ts">
import { PropType, defineComponent } from "vue";
import Modal from "../Modal.vue";

export default defineComponent({
  props: {
    title: String,
    sidebar: String as PropType<"left" | "right">,
    btnClass: String,
    ariaLabel: String,
  },
  data() {
    return { show: false };
  },
  components: { Modal },
  methods: {
    closeDialog() {
      this.show = false;
    },
  },
});
</script>
