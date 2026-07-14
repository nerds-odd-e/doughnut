<template>
  <ul
    v-if="!usePortal"
    ref="panelRef"
    tabindex="0"
    :class="panelClasses"
  >
    <slot />
  </ul>
  <Teleport v-else-if="portalOpen" :to="teleportTarget">
    <ul
      ref="panelRef"
      tabindex="0"
      :class="panelClasses"
      data-dropdown-portal-panel
      :data-dropdown-portal-for="portalContext!.portalId"
      :style="portalPositionStyle"
    >
      <slot />
    </ul>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, inject, ref, type StyleValue } from "vue"
import {
  dropdownPortalContextKey,
  dropdownPortalTeleportTarget,
} from "@/composables/dropdownPortalContext"
import { useDropdownPortalPosition } from "@/composables/useDropdownPortalPosition"
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

const portalContext = inject(dropdownPortalContextKey, null)
const usePortal = computed(() => portalContext != null)
const portalOpen = computed(() => portalContext?.open.value ?? false)
const teleportTarget = computed(() =>
  dropdownPortalTeleportTarget(portalContext?.detailsRef.value)
)

const panelRef = ref<HTMLElement | null>(null)

const panelClasses = computed(() =>
  [dropdownMenuPanelClass(props.size), props.panelClass]
    .filter(Boolean)
    .join(" ")
)

const { positionStyle } = portalContext
  ? useDropdownPortalPosition({
      open: portalContext.open,
      detailsRef: portalContext.detailsRef,
      panelRef,
    })
  : { positionStyle: ref<{ top: string; left: string } | null>(null) }

const portalPositionStyle = computed((): StyleValue => {
  const pos = positionStyle.value
  if (!pos) {
    return {
      position: "fixed",
      top: "0",
      left: "0",
      opacity: "0",
      pointerEvents: "none",
    }
  }
  return {
    position: "fixed",
    top: pos.top,
    left: pos.left,
    opacity: "1",
    pointerEvents: "auto",
  }
})
</script>
