<template>
  <details ref="detailsRef" v-bind="$attrs">
    <slot :closeDropdown="closeDropdown"></slot>
  </details>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useAutoCollapseDetails } from "@/composables/useAutoCollapseDetails"

defineOptions({ inheritAttrs: false })

const detailsRef = ref<HTMLDetailsElement | null>(null)
const { closeDetails: closeDropdown } = useAutoCollapseDetails(detailsRef)

defineExpose({ closeDropdown })
</script>
