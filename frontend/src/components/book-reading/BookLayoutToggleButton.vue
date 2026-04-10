<template>
  <button
    type="button"
    class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0"
    :class="{ 'sidebar-expanded': opened }"
    data-testid="book-reading-book-layout-toggle"
    aria-label="Book layout"
    title="Book layout"
    :aria-expanded="opened"
    :aria-controls="panelId"
    @click="opened = !opened"
  >
    <div class="daisy-w-4 daisy-h-4">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="w-4 h-4"
      >
        <template v-if="opened">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </template>
        <template v-else>
          <line x1="3" y1="6" x2="21" y2="6" />
          <line x1="6" y1="12" x2="21" y2="12" />
          <line x1="3" y1="18" x2="21" y2="18" />
        </template>
      </svg>
    </div>
  </button>
</template>

<script setup lang="ts">
const opened = defineModel<boolean>("opened", { required: true })

withDefaults(
  defineProps<{
    panelId?: string
  }>(),
  { panelId: "book-reading-book-layout-panel" }
)
</script>
