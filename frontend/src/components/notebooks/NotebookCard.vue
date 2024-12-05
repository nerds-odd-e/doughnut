<template>
  <div role="card" class="card notebook-card">
    <div class="notebook-binding"></div>
    <slot name="cardHeader" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: notebook.headNoteId } }"
      class="text-decoration-none"
    >
      <div class="card-body">
        <h5 class="card-title">
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
import type { Notebook } from "@/generated/backend"

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
  background: linear-gradient(to right, #f5f5f5 0%, #ffffff 5%);
  height: 200px;
  overflow: hidden;
}

.notebook-binding {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 20px;
  background: linear-gradient(to right,
    #e0e0e0 0%,
    #f0f0f0 50%,
    #e0e0e0 100%
  );
  border-right: 1px solid #ccc;
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
    background: #fff;
    border: 1px solid #ccc;
    border-radius: 50%;
  }

  &::before {
    top: 20%;
  }

  &::after {
    bottom: 20%;
  }
}

.card-body {
  margin-left: 20px;
  padding: 1rem 1.25rem;
}

.notebook-card:hover {
  background: linear-gradient(to right, #f0f0f0 0%, #f8f9fa 5%);
  transform: translateY(-2px);
  transition: all 0.2s ease;
}

/* Add ruled paper effect */
.card-body {
  background-image: linear-gradient(#e5e5e5 1px, transparent 1px);
  background-size: 100% 2rem;
  background-position: 0 1rem;
}

.card-title {
  color: #2c3e50;
  margin-bottom: 1.5rem;
}

.note-short-details {
  color: #666;
  line-height: 2rem; /* Align with ruled lines */
}
</style>
