<template>
  <router-link
    :to="{ name }"
    class="nav-item"
    :class="{ active: isActive }"
  >
    <div class="icon-container">
      <component :is="icon" width="24" height="24" />
      <div v-if="badge" :class="badgeClass">
        {{ badge }}
      </div>
    </div>
    <span class="label">{{ label }}</span>
  </router-link>
</template>

<script setup lang="ts">
import type { Component } from "vue"

defineProps<{
  name: string
  label: string
  icon: Component
  isActive: boolean
  badge?: number
  badgeClass?: string
}>()
</script>

<style lang="scss" scoped>
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: inherit;
  gap: 0.5rem;

  &.active {
    color: #66b0ff;
  }
}

.icon-container {
  position: relative;
  width: 24px;
  height: 24px;
}

.due-count, .recall-count, .unread-count {
  position: absolute;
  top: -6px;
  right: -6px;
  border-radius: 50%;
  min-width: 16px;
  height: 16px;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  color: white;
}

.due-count {
  background: #66b0ff;
}

.recall-count {
  background: #4CAF50;
}

.unread-count {
  background: #d07027;
}

.label {
  font-size: 0.8rem;
}
</style>
