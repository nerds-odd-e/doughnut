<template>
  <a class="btn btn-sm" role="button" @click="show = true" :title="title">
    <slot name="button_face" />
    <template v-if="!$slots.button_face">
      {{ title }}
    </template>
    <Modal v-if="show" :sidebar="sidebar" @close_request="show = false">
      <template #body>
        <slot />
      </template>
    </Modal>
  </a>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import Modal from "../Modal.vue";

export default defineComponent({
  props: { title: String, sidebar: String as PropType<"left" | "right"> },
  data() {
    return { show: false };
  },
  components: { Modal },
});
</script>
