<!-- eslint-disable vue/valid-template-root -->
<template>
  <Modal
    class="popups"
    :sidebar="sidebar"
    @close_request="$emit('popupDone', $event)"
  >
    <template #body>
      <slot />
    </template>
  </Modal>
</template>

<script lang="ts">
//
// Usage:
//  <Popup v-if="showThisDialog">
//    <h2>Dialog Title</h2>
//     <button @click="popup.done()">OK</button>
//  </Popup>
//
import { PropType, defineComponent } from "vue";

export default defineComponent({
  props: {
    sidebar: String as PropType<"left" | "right">,
  },
  emits: ["popupDone"],
});
</script>

<style lang="scss" scoped>
.popups {
  z-index: 9998;
}
</style>
