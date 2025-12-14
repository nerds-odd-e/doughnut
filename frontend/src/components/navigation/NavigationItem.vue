<template>
  <div
    class="nav-item daisy-text-neutral-content daisy-rounded-lg daisy-px-2"
    :class="{
      'daisy-text-primary daisy-bg-primary/10': isActive && name !== 'resumeRecall' && !nonClickable,
      'resume-recall-active': name === 'resumeRecall' && isActive,
      'hover:daisy-bg-base-content/5': !isActive && !nonClickable,
      'non-clickable': nonClickable
    }"
  >
    <div
      v-if="nonClickable"
      class="daisy-flex daisy-flex-col daisy-items-center"
      :aria-label="label"
    >
      <div class="icon-container">
        <component :is="icon" width="24" height="24" />
        <div v-if="badge" :class="badgeClass">
          {{ badge }}
        </div>
      </div>
      <span class="label">{{ label }}</span>
    </div>
    <router-link
      v-else-if="name && !hasDropdown && name !== 'resumeRecall'"
      :to="{ name: name }"
      :aria-label="label"
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
    <a
      v-else-if="name === 'resumeRecall' && !hasDropdown"
      :aria-label="label"
      class="daisy-flex daisy-flex-col daisy-items-center"
      @click.prevent="$emit('resumeRecall')"
    >
      <div class="icon-container">
        <component :is="icon" width="24" height="24" />
        <div v-if="badge" :class="badgeClass">
          {{ badge }}
        </div>
      </div>
      <span class="label">{{ label }}</span>
    </a>

    <details v-if="hasDropdown && !nonClickable" ref="dropdownTrigger" class="daisy-dropdown daisy-dropdown-bottom daisy-dropdown-end lg:daisy-dropdown-top lg:daisy-dropdown-right">
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
import { ref, onMounted, onUnmounted } from "vue"

const props = defineProps<{
  name?: string
  label: string
  icon: Component
  isActive: boolean
  badge?: number
  badgeClass?: string
  hasDropdown?: boolean
  nonClickable?: boolean
}>()

defineEmits<{
  (e: "resumeRecall"): void
}>()

const dropdownTrigger = ref<HTMLDetailsElement | null>(null)

const closeDropdown = () => {
  if (dropdownTrigger.value) {
    dropdownTrigger.value.open = false
  }
}

const handleClickOutside = (event: MouseEvent) => {
  if (dropdownTrigger.value && dropdownTrigger.value.open) {
    const target = event.target as Node
    if (!dropdownTrigger.value.contains(target)) {
      closeDropdown()
    }
  }
}

// Only register click-outside listener for dropdown items
onMounted(() => {
  if (props.hasDropdown) {
    document.addEventListener("click", handleClickOutside)
  }
})

onUnmounted(() => {
  if (props.hasDropdown) {
    document.removeEventListener("click", handleClickOutside)
  }
})
</script>

<style lang="scss" scoped>
.nav-item {
  text-decoration: none;
  transition: background-color 0.2s ease;
  position: relative;
}

.nav-item.non-clickable {
  cursor: default;
  pointer-events: none;
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

  &.diligent-mode {
    background: #dc2626;
  }
}

.unread-count {
  background: #d07027;
}

.resume-recall-active {
  background-color: #4CAF50;
  color: white;
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
