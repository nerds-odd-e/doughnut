<template>
  <router-link
    :to="
      focusedPropertyKey
        ? notePropertyLocation(noteTopology.id, focusedPropertyKey)
        : noteShowLocation(noteTopology.id)
    "
    class="no-underline"
  >
    <NoteTitleComponent v-bind="{ noteTopology }" />
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/donut-backend-api"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  focusedPropertyKey: {
    type: String as PropType<string | undefined>,
    default: undefined,
  },
})
</script>
