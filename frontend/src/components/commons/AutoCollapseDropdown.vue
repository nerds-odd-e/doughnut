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
import { provide, ref, useId } from "vue"
import { useAutoCollapseDetails } from "@/composables/useAutoCollapseDetails"
import {
  dropdownPortalContextKey,
  type DropdownPortalContext,
} from "@/composables/dropdownPortalContext"

defineOptions({ inheritAttrs: false })

const portalId = useId()
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

const portalContext: DropdownPortalContext = {
  portalId,
  open,
  detailsRef,
}
provide(dropdownPortalContextKey, portalContext)

useAutoCollapseDetails(detailsRef, closeDropdown, portalId)

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
