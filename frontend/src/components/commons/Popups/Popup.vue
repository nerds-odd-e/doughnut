<!-- eslint-disable vue/valid-template-root -->
<template></template>

<script lang="ts">
//
// Usage:
//  <Pop v-model="showThisDialog">
//    <h2>Dialog Title</h2>
//     <button @click="popup.done()">OK</button>
//  </Pop>
//
import { PropType, defineComponent } from "vue";
import usePopups from "./usePopups";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { modelValue: Boolean, sidebar: String as PropType<"left" | "right"> },
  emits: ["update:modelValue"],
  watch: {
    modelValue() {
      if (!this.modelValue) return;
      this.popups
        .dialog(this.$slots.default, this.sidebar)
        .then(() => this.$emit("update:modelValue", false));
    },
  },
});
</script>
