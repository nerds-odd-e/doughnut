<template>
  <div
    class="nav-item text-neutral-content rounded-lg px-2"
    :class="{
      'text-primary bg-primary/10': isActive && !isNavigationActionItem(name) && !nonClickable,
      'resume-recall-active': name === 'resumeRecall' && isActive,
      'hover:bg-base-content/5': !isActive && !nonClickable,
      'non-clickable': nonClickable
    }"
  >
    <div
      v-if="nonClickable"
      class="flex flex-col items-center"
      :aria-label="label"
    >
      <div class="icon-container">
        <component :is="icon" :size="24" />
        <div v-if="badge" :class="badgeClass" :title="badgeTitle">
          {{ badge }}
        </div>
      </div>
      <span class="label">{{ label }}</span>
      <AssimilationMenuProgress v-if="name === 'assimilate'" />
    </div>
    <router-link
      v-else-if="name && !isNavigationActionItem(name)"
      :to="{ name: name }"
      :aria-label="label"
      class="flex flex-col items-center"
    >
      <div class="icon-container">
        <component :is="icon" :size="24" />
        <div v-if="badge" :class="badgeClass" :title="badgeTitle">
          {{ badge }}
        </div>
      </div>
      <span class="label">{{ label }}</span>
      <AssimilationMenuProgress v-if="name === 'assimilate'" />
    </router-link>
    <a
      v-else-if="isNavigationActionItem(name)"
      :aria-label="label"
      tabindex="0"
      class="flex flex-col items-center"
      @click.prevent="onActionClick"
      @keydown.enter.prevent="onActionClick"
    >
      <div class="icon-container">
        <component :is="icon" :size="24" />
        <div v-if="badge" :class="badgeClass" :title="badgeTitle">
          {{ badge }}
        </div>
      </div>
      <span class="label">{{ label }}</span>
      <AssimilationMenuProgress v-if="name === 'assimilate'" />
    </a>

    <button
      v-else
      type="button"
      :aria-label="label"
      :aria-expanded="expanded"
      class="flex flex-col items-center w-full cursor-pointer"
      @click="emit('toggle')"
    >
      <div class="icon-container">
        <component :is="icon" :size="24" />
      </div>
      <span class="label">{{ label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import AssimilationMenuProgress from "@/components/navigation/AssimilationMenuProgress.vue"
import { isNavigationActionItem } from "@/components/navigation/navigationActionItems"
import type { NavigationItemProps } from "@/components/navigation/navigationItem"

const { name } = defineProps<NavigationItemProps>()

const emit = defineEmits<{
  (e: "resumeRecall"): void
  (e: "goToNextAssimilation"): void
  (e: "toggle"): void
}>()

const onActionClick = () => {
  if (name === "resumeRecall") {
    emit("resumeRecall")
  } else if (name === "assimilate") {
    emit("goToNextAssimilation")
  }
}
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
</style>
