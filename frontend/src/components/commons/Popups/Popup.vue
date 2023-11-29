<!-- eslint-disable vue/valid-template-root -->
<template>
  <Popup1 v-bind="{ show, sidebar }" @popup-done="$emit('popupDone', $event)">
    <slot />
  </Popup1>
</template>

<script lang="ts">
//
// Usage:
//  <Popup v-model="showThisDialog">
//    <h2>Dialog Title</h2>
//     <button @click="popup.done()">OK</button>
//  </Popup>
//
import { PropType, defineComponent } from "vue";
import usePopups from "./usePopups";
import Popup1 from "./Popup1.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { show: Boolean, sidebar: String as PropType<"left" | "right"> },
  emits: ["popupDone"],
  components: { Popup1 },
});
</script>
