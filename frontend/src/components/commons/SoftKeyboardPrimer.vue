<template>
  <input
    :id="softKeyboardPrimerId"
    type="text"
    class="sr-only"
    tabindex="-1"
    aria-hidden="true"
    autocomplete="off"
    :data-testid="softKeyboardPrimerId"
  />
</template>

<script setup lang="ts">
import { softKeyboardPrimerId } from "@/utils/focusTarget"
</script>
