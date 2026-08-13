<template>
  <button
    type="button"
    :class="multiline ? dropdownMenuButtonMultilineClass : dropdownMenuButtonClass"
    :title="title"
    :data-catalog-sort="catalogSort"
    @click="$emit('click', $event)"
  >
    <component
      v-if="icon"
      :is="icon"
      :size="iconSize"
      :class="iconClass"
      aria-hidden="true"
    />
    <span :class="labelClass">
      <slot>{{ title }}</slot>
    </span>
  </button>
</template>

<script setup lang="ts">
import type { Component } from "vue"
import { computed } from "vue"
import {
  dropdownMenuButtonClass,
  dropdownMenuButtonMultilineClass,
} from "./dropdownMenuClasses"

const props = withDefaults(
  defineProps<{
    title: string
    icon?: Component
    iconSize?: number
    iconClass?: string
    multiline?: boolean
    catalogSort?: string
  }>(),
  {
    iconSize: 20,
    iconClass: "shrink-0",
    multiline: false,
  }
)

defineEmits<{
  (e: "click", event: MouseEvent): void
}>()

const labelClass = computed(() =>
  props.multiline ? "min-w-0 text-left leading-snug" : undefined
)
</script>
