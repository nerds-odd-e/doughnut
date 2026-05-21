<template>
  <div
    class="notebook-list-row rounded-box border border-base-300/80 bg-base-100 transition-colors hover:border-primary/30 hover:bg-base-200/40"
    :class="{
      'notebook-list-row--compact': compact,
    }"
    data-cy="notebook-card"
  >
    <div
      class="notebook-card notebook-list-row__shell flex flex-row items-stretch gap-3"
      :class="
        compact ? 'px-3 py-2 gap-2' : 'px-4 py-3'
      "
    >
      <router-link
        :to="{ name: 'notebookPage', params: { notebookId: notebook.id } }"
        class="notebook-list-row__main flex min-w-0 flex-1 flex-col justify-center gap-0.5 no-underline"
      >
        <h5
          class="font-semibold leading-snug text-base-content truncate"
          :class="compact ? 'text-sm' : 'text-base'"
        >
          {{ notebook.name }}
        </h5>
        <p
          v-if="notebook.description"
          class="m-0 line-clamp-2 leading-relaxed text-base-content/60"
          :class="compact ? 'text-xs' : 'text-sm'"
        >
          {{ notebook.description }}
        </p>
      </router-link>
      <div
        class="notebook-list-row__toolbar flex shrink-0 items-center self-center"
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
