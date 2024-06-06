<template>
  <h3>Assessment For LeSS in Action</h3>
  <p>{{ result }}</p>
  <p>{{ errors }}</p>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  data() {
    return {
      result: {},
      errors: {},
    };
  },
});
</script>
