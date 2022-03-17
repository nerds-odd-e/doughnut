<template>
      <button
        class="btn btn-sm"
        role="button"
        @click="showDialog"
        :title="title"
      >
      <slot name="face"/>
      <template v-if="!$slots.face">
        {{ title }}
      </template>
      </button>

</template>

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "./usePopup";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { title: String },
  methods: {
    showDialog() {
      this.popups.dialog(this.$slots.default)
    },
  },
});
</script>
