<template>
  <DropdownMenu size="wide">
    <DropdownMenuItem
      v-for="row in SIDEBAR_PEER_SORT_MENU_ROWS"
      :key="`${row.spec.field}-${row.spec.direction}`"
    >
      <DropdownMenuActionButton
        multiline
        :title="row.label"
        :icon="row.Icon"
        :icon-size="14"
        icon-class="mt-0.5 shrink-0"
        :catalog-sort="
          catalogSortButtons
            ? `${row.spec.field}-${row.spec.direction}`
            : undefined
        "
        @click="$emit('select', row.spec)"
      >
        {{ row.label }}
      </DropdownMenuActionButton>
    </DropdownMenuItem>
  </DropdownMenu>
</template>

<script setup lang="ts">
import { SIDEBAR_PEER_SORT_MENU_ROWS } from "@/composables/sidebarPeerSortMenuRows"
import type { SidebarPeerSortSpec } from "@/composables/useNoteSidebarPeerSort"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"

defineEmits<{
  (e: "select", spec: SidebarPeerSortSpec): void
}>()

defineProps<{
  catalogSortButtons?: boolean
}>()
</script>
