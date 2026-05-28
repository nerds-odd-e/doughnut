<template>
  <ul tabindex="0" :class="panelClasses">
    <slot />
  </ul>
</template>

<script setup lang="ts">
import { computed } from "vue"
import {
  dropdownMenuPanelClass,
  type DropdownMenuSize,
} from "./dropdownMenuClasses"

const props = withDefaults(
  defineProps<{
    size?: DropdownMenuSize
    panelClass?: string
  }>(),
  { size: "narrow" }
)

const panelClasses = computed(() =>
  [dropdownMenuPanelClass(props.size), props.panelClass]
    .filter(Boolean)
    .join(" ")
)
</script>
