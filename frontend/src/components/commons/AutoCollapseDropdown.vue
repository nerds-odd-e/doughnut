<template>
  <details
    ref="detailsRef"
    data-auto-collapse-dropdown
    :class="{ 'daisy-dropdown-open': open }"
    v-bind="$attrs"
    @click="onClick"
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
const setOpen = (isOpen: boolean) => {
  if (detailsRef.value) {
    detailsRef.value.open = isOpen
  }
  open.value = isOpen
}

const closeDropdown = () => setOpen(false)
const toggleDropdown = () => setOpen(!open.value)
useAutoCollapseDetails(detailsRef, closeDropdown)

const onClick = (event: MouseEvent) => {
  if (event.target === detailsRef.value) {
    toggleDropdown()
  }
}

const onToggle = (event: Event) => {
  open.value = (event.target as HTMLDetailsElement).open
}

defineExpose({ closeDropdown })
</script>
