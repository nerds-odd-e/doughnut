<template>
  <li :class="dropdownMenuItemClass">
    <slot />
  </li>
</template>

<script setup lang="ts">
import { dropdownMenuItemClass } from "./dropdownMenuClasses"
</script>
