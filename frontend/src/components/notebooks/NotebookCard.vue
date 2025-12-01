<template>
  <div class="notebook-card">
    <div class="notebook-binding"></div>
    <slot name="cardHeader" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: notebook.headNoteId } }"
      class="no-underline"
    >
      <div class="daisy-p-4">
        <h5 class="daisy-text-lg daisy-font-semibold">
          {{ notebook.title }}
        </h5>
        <p v-if="notebook.shortDetails" class="note-short-details">
          {{ notebook.shortDetails }}
        </p>
      </div>
    </router-link>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@generated/backend"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})
</script>

<style scoped>
.notebook-card {
  position: relative;
  border-radius: 3px;
  border-left: none;
  box-shadow: 2px 2px 5px rgba(0,0,0,0.1);
  background: linear-gradient(to right, oklch(var(--bc) / 0.2) 0%, oklch(var(--b2) / 0.8) 5%);
  height: 200px;
  overflow: hidden;
  margin-bottom: 1rem;
}

.notebook-binding {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 12px;
  background: oklch(var(--b3));
  border-right: 1px solid oklch(var(--bc) / 0.25);
  border-radius: 3px 0 0 3px;

  /* Add notebook holes */
  &::before,
  &::after {
    content: '';
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    width: 8px;
    height: 8px;
    background: oklch(var(--b1));
    border: 1px solid oklch(var(--bc) / 0.2);
    border-radius: 50%;
  }

  &::before {
    top: 20%;
  }

  &::after {
    bottom: 20%;
  }
}

.note-short-details {
  color: oklch(var(--bc) / 0.6);
  line-height: 2rem; /* Align with ruled lines */
}
</style>
