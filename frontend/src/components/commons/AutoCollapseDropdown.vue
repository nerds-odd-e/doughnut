<template>
  <details
    ref="detailsRef"
    data-auto-collapse-dropdown
    v-bind="$attrs"
    @toggle="onToggle"
  >
    <slot :closeDropdown="closeDropdown" :open="open"></slot>
  </details>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useAutoCollapseDetails } from "@/composables/useAutoCollapseDetails"

defineOptions({ inheritAttrs: false })

const detailsRef = ref<HTMLDetailsElement | null>(null)
const open = ref(false)
const { closeDetails: closeDropdown } = useAutoCollapseDetails(detailsRef)

const onToggle = (event: Event) => {
  open.value = (event.target as HTMLDetailsElement).open
}

defineExpose({ closeDropdown })
</script>
