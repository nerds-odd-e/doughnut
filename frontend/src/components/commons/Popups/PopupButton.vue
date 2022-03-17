<template>
      <button
        class="btn btn-sm"
        role="button"
        @click="showDialog"
        :title="title"
      >
        {{ title }}
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
      this.popups.dialog1(this.$slots.default)
    },
  },
});
</script>
