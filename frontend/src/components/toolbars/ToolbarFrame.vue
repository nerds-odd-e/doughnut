<template>
  <nav class="navbar toolbar">
    <div class="btn-group btn-group-sm">
      <slot />
    </div>
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
