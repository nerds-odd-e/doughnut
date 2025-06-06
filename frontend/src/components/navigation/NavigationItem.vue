<template>
  <div
    class="nav-item daisy-text-neutral-content daisy-rounded-lg daisy-px-2"
    :class="{
      'daisy-text-primary daisy-bg-primary/10': isActive,
      'hover:daisy-bg-base-content/5': !isActive,
      'daisy-dropdown daisy-dropdown-end lg:daisy-dropdown-right': hasDropdown
    }"
  >
    <router-link
      v-if="name && !hasDropdown"
      :to="{ name: name }"
      class="daisy-flex daisy-flex-col daisy-items-center"
    >
      <div class="icon-container">
        <component :is="icon" width="24" height="24" />
        <div v-if="badge" :class="badgeClass">
          {{ badge }}
        </div>
      </div>
      <span class="label">{{ label }}</span>
    </router-link>

    <details v-if="hasDropdown" ref="dropdownTrigger" class="daisy-dropdown">
      <summary
        tabindex="0"
        role="button"
        class="daisy-flex daisy-flex-col daisy-items-center cursor-pointer list-none"
        :aria-label="label"
      >
        <div class="icon-container">
          <component :is="icon" width="24" height="24" />
          <div v-if="badge" :class="badgeClass">
            {{ badge }}
          </div>
        </div>
        <span class="label">{{ label }}</span>
      </summary>

      <slot name="dropdown" :closeDropdown="closeDropdown"></slot>
    </details>
  </div>
</template>

<script setup lang="ts">
import type { Component } from "vue"
import { ref } from "vue"

const dropdownTrigger = ref<HTMLDetailsElement | null>(null)

const closeDropdown = () => {
  if (dropdownTrigger.value) {
    dropdownTrigger.value.open = false
  }
}

defineProps<{
  name?: string
  label: string
  icon: Component
  isActive: boolean
  badge?: number
  badgeClass?: string
  hasDropdown?: boolean
}>()
</script>

<style lang="scss" scoped>
.nav-item {
  text-decoration: none;
  transition: background-color 0.2s ease;
  position: relative;
}

.nav-item a {
  text-decoration: none;
  color: inherit;
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

summary::marker {
  display: none;
}
summary::-webkit-details-marker {
  display: none;
}
</style>
