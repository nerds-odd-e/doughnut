<template>
  <nav class="navbar toolbar justify-content-between">
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
}
</style>
