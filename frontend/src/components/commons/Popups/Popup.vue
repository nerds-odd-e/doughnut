<!-- eslint-disable vue/valid-template-root -->
<template></template>

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

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: {
    sidebar: String as PropType<"left" | "right">,
  },
  emits: ["popupDone"],
  mounted() {
    this.popups
      .dialog(this.$slots.default, this.sidebar)
      .then((result) => this.$emit("popupDone", result));
  },
});
</script>
