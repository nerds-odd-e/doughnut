<template>
  <div
    class="notebook-list-row daisy-rounded-box daisy-border daisy-border-base-300/80 daisy-bg-base-100 daisy-transition-colors hover:daisy-border-primary/30 hover:daisy-bg-base-200/40"
    :class="{ 'notebook-list-row--subscribed': isSubscribed }"
    data-cy="notebook-card"
  >
    <div class="notebook-card notebook-list-row__shell daisy-flex daisy-flex-row daisy-items-stretch daisy-gap-3 daisy-px-4 daisy-py-3">
      <router-link
        :to="{ name: 'noteShow', params: { noteId: notebook.headNoteId } }"
        class="notebook-list-row__main daisy-flex daisy-min-w-0 daisy-flex-1 daisy-flex-col daisy-justify-center daisy-gap-0.5 daisy-no-underline"
      >
        <h5 class="daisy-text-base daisy-font-semibold daisy-leading-snug daisy-text-base-content daisy-truncate">
          {{ notebook.title }}
        </h5>
        <p
          v-if="notebook.shortDetails"
          class="daisy-m-0 daisy-line-clamp-2 daisy-text-sm daisy-leading-relaxed daisy-text-base-content/60"
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
  isSubscribed?: boolean
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

.notebook-list-row.notebook-list-row--subscribed {
  background: linear-gradient(
    to right,
    oklch(var(--p) / 0.2) 0%,
    oklch(var(--p) / 0.1) 5%
  );
}

.notebook-list-row.notebook-list-row--subscribed:hover {
  background: linear-gradient(
    to right,
    oklch(var(--p) / 0.24) 0%,
    oklch(var(--p) / 0.14) 5%
  );
}
</style>
