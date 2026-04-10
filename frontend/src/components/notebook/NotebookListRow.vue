<template>
  <div
    class="notebook-list-row daisy-rounded-box daisy-border daisy-border-base-300/80 daisy-bg-base-100 daisy-transition-colors hover:daisy-border-primary/30 hover:daisy-bg-base-200/40"
    :class="{
      'notebook-list-row--compact': compact,
    }"
    data-cy="notebook-card"
  >
    <div
      class="notebook-card notebook-list-row__shell daisy-flex daisy-flex-row daisy-items-stretch daisy-gap-3"
      :class="
        compact ? 'daisy-px-3 daisy-py-2 daisy-gap-2' : 'daisy-px-4 daisy-py-3'
      "
    >
      <router-link
        :to="{ name: 'noteShow', params: { noteId: notebook.headNoteId } }"
        class="notebook-list-row__main daisy-flex daisy-min-w-0 daisy-flex-1 daisy-flex-col daisy-justify-center daisy-gap-0.5 daisy-no-underline"
      >
        <h5
          class="daisy-font-semibold daisy-leading-snug daisy-text-base-content daisy-truncate"
          :class="compact ? 'daisy-text-sm' : 'daisy-text-base'"
        >
          {{ notebook.title }}
        </h5>
        <p
          v-if="notebook.shortDetails"
          class="daisy-m-0 daisy-line-clamp-2 daisy-leading-relaxed daisy-text-base-content/60"
          :class="compact ? 'daisy-text-xs' : 'daisy-text-sm'"
        >
          {{ notebook.shortDetails }}
        </p>
      </router-link>
      <div
        class="notebook-list-row__toolbar daisy-flex daisy-shrink-0 daisy-items-center daisy-self-center"
        @click.stop
      >
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Notebook } from "@generated/doughnut-backend-api"

defineProps<{
  notebook: Notebook
  compact?: boolean
}>()
</script>

<style scoped>
.notebook-card {
  margin: 0;
  border: none;
  border-radius: inherit;
  box-shadow: none;
  background: transparent;
}
</style>
