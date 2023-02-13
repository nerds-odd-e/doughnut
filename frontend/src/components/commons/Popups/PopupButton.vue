<template>
  <a class="btn btn-sm" role="button" @click="showDialog" :title="title">
    <slot name="button_face" />
    <template v-if="!$slots.button_face">
      {{ title }}
    </template>
  </a>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "./usePopups";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { title: String, sidebar: Boolean },
  methods: {
    showDialog() {
      this.popups.dialog(this.$slots.dialog_body, this.sidebar);
    },
  },
});
</script>
