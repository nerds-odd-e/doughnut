<template>
      <button
        class="btn btn-sm"
        role="button"
        @click="showDialog"
        :title="title"
      >
      {{icon}}
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
  props: { title: String, icon: Object },
  methods: {
    showDialog() {
      this.popups.dialog1(this.$slots.default)
    },
  },
});
</script>
