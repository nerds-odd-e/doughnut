<template>
  <div
    class="flex min-w-0 items-center gap-2"
    :class="value.trim() ? '' : 'justify-between'"
  >
          <template v-if="value.trim()">
            <button
              type="button"
              class="daisy-btn daisy-btn-ghost daisy-btn-sm h-auto min-h-0 min-w-0 max-w-full shrink truncate justify-start py-0.5 px-1 font-mono text-sm font-normal text-base-content/90 normal-case"
              :title="value.trim()"
              data-testid="rich-note-wikidata-property-edit"
              :aria-label="`Edit Wikidata ID ${value.trim()}`"
              @click="emit('edit')"
            >
              {{ value.trim() }}
            </button>
            <RichFrontmatterPropertyExternalLink
              kind="wikidata"
              :value="value"
            />
          </template>
          <template v-else>
            <span
              class="truncate font-mono text-sm text-base-content/90"
              aria-hidden="true"
              >—</span
            >
            <button
              type="button"
              class="daisy-btn daisy-btn-sm daisy-btn-outline shrink-0"
              data-testid="rich-note-wikidata-property-edit"
              aria-label="Set Wikidata ID"
              @click="emit('edit')"
            >
              Set…
            </button>
          </template>
  </div>
</template>

<script setup lang="ts">
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"

defineProps<{ value: string }>()
const emit = defineEmits<{ edit: [] }>()
</script>
