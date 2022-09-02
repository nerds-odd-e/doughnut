<template>
  <nav class="toolbar navbar justify-content-between">
    <slot />
  </nav>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({});
</script>

<style lang="scss">
.btn:disabled {
  border-color: transparent;
}
</style>

<style scoped lang="scss">
.toolbar {
  background-color: #cee0fa;
  padding: 0px 5px;
}
</style>
