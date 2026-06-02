<template>
  <NoteMoreOptionsActions
    v-if="inline"
    layout="toolbar"
    v-bind="{ note }"
  />

  <AutoCollapseDropdown
    v-else
    v-slot="{ closeDropdown, open }"
    ref="overflowDropdownRef"
    class="daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom"
  >
    <summary
      :class="[
        'daisy-btn daisy-btn-ghost daisy-btn-sm list-none cursor-pointer',
        { 'daisy-btn-active': open },
      ]"
      title="more options"
      aria-label="more options"
    >
      <MoreHorizontal class="w-6 h-6" />
    </summary>
    <NoteMoreOptionsForm
      v-bind="{ note }"
      @close-dialog="closeDropdown"
    />
  </AutoCollapseDropdown>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { MoreHorizontal } from "@lucide/vue"
import { ref } from "vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import NoteMoreOptionsActions from "./NoteMoreOptionsActions.vue"
import NoteMoreOptionsForm from "./NoteMoreOptionsForm.vue"

defineProps<{
  note: Note
  inline: boolean
}>()

const overflowDropdownRef = ref<InstanceType<
  typeof AutoCollapseDropdown
> | null>(null)

const closeOverflowMenu = () => {
  overflowDropdownRef.value?.closeDropdown()
}

defineExpose({ closeOverflowMenu })
</script>
