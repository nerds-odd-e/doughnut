<template>
  <h3>Assessment</h3>
  <p v-if="!loggedIn">Please login first</p>

  <p v-else>Assessment For LeSS in Action</p>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Notebook } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
    loggedIn: Boolean,
  },
  data() {
    return {
      errors: {},
    };
  },
});
</script>
