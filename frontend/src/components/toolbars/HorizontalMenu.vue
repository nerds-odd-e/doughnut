<template>
  <div class="sidebar-container daisy-w-full daisy-h-full">
    <div
      ref="menuRef"
      tabindex="0"
      class="menu-wrapper daisy-flex daisy-flex-col daisy-h-full"
      :class="{ 'is-expanded': isExpanded, 'is-collapsed': !isExpanded }"
      @blur="handleFocusLoss"
    >
      <div class="menu-content daisy-flex daisy-flex-row daisy-items-center">
        <!-- Login button: always visible when no user -->
        <LoginButton v-if="!user" />

        <!-- User menu content -->
        <template v-if="user">
          <!-- Collapsed state: show only active item (not on home page) -->
          <template v-if="!isExpanded && hasActiveItem && !isHomePage && activeItem">
            <li class="daisy-menu-item active-item-only">
              <NavigationItem v-bind="{ ...activeItem }" />
            </li>
          </template>

          <!-- Expanded state: show all items -->
          <ul v-if="isExpanded" class="top-menu daisy-menu daisy-flex-1">
            <template v-if="!isHomePage">
              <li v-for="item in upperNavItems" :title="item.label" :key="item.name" class="daisy-menu-item">
                <NavigationItem v-bind="{ ...item }" />
              </li>
            </template>

            <template v-if="!isHomePage">
              <li v-for="item in lowerNavItems" :title="item.label" :key="item.name" class="daisy-menu-item">
                <NavigationItem v-bind="{ ...item, to: item.name }" />
              </li>
            </template>

            <AccountMenuItem
              :user="user"
              :show-user-settings-dialog="showUserSettingsDialog"
              :logout="logout"
            />
          </ul>
        </template>

        <!-- Expand button: always visible -->
        <button
          class="expand-button daisy-btn daisy-btn-ghost daisy-btn-sm"
          @click="toggleExpanded"
          aria-label="Toggle menu"
        >
          <SvgChevronRight />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { User } from "@generated/backend"
import type { PropType, Component, Ref } from "vue"
import { ref, computed, onMounted, onUnmounted } from "vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import SvgChevronRight from "@/components/svgs/SvgChevronRight.vue"

type NavigationItemType = {
  name?: string
  label: string
  icon: Component
  isActive: boolean
  badge?: number
  badgeClass?: string
  hasDropdown?: boolean
}

const props = defineProps({
  user: { type: Object as PropType<User>, required: false },
  upperNavItems: {
    type: Array as PropType<NavigationItemType[]>,
    required: true,
  },
  lowerNavItems: {
    type: Array as PropType<NavigationItemType[]>,
    required: true,
  },
  isHomePage: { type: Boolean, required: true },
  showUserSettingsDialog: {
    type: Function as PropType<() => void>,
    required: true,
  },
  logout: { type: Function as PropType<() => void>, required: true },
})

const isExpanded = ref(false)
const menuRef: Ref<HTMLElement | null> = ref(null)

const activeItem = computed(() => {
  const allItems = [...props.upperNavItems, ...props.lowerNavItems]
  return allItems.find((item) => item.isActive)
})

const hasActiveItem = computed(() => {
  return activeItem.value !== undefined
})

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const collapseMenu = () => {
  isExpanded.value = false
}

const handleClickOutside = (event: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(event.target as Node)) {
    collapseMenu()
  }
}

const handleFocusLoss = () => {
  // Use setTimeout to allow click events to process first
  setTimeout(() => {
    if (menuRef.value && document.activeElement !== menuRef.value) {
      collapseMenu()
    }
  }, 0)
}

onMounted(() => {
  document.addEventListener("click", handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener("click", handleClickOutside)
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  height: auto;
  display: block;
  width: 100%;
}

.menu-wrapper {
  position: relative;
  outline: none;
  display: flex;
  align-items: center;
  width: 100%;
}

.menu-wrapper.is-collapsed {
  width: auto;
}

.menu-content {
  width: 100%;
  transition: all 0.3s ease;
  align-items: center;
  min-height: 100%;
  display: flex;
}

.is-collapsed .menu-content {
  width: auto;
}

.is-collapsed .menu-content {
  justify-content: flex-start;
}

.is-expanded .menu-content {
  justify-content: center;
}

.daisy-menu-item {
  padding: 0;
  text-align: center;
  width: auto;
  display: flex;
  justify-content: center;
  align-items: center;

  :deep(.navigation-item) {
    width: 100%;
    display: flex;
    justify-content: center;
  }

  .label {
    font-size: 0.8rem;
    line-height: 1;
  }
}

.active-item-only {
  flex-shrink: 0;
  min-width: fit-content;
  display: flex;
  align-items: center;
}

.top-menu {
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 1rem;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.is-collapsed .top-menu {
  opacity: 0;
  transform: translateX(-20px);
  pointer-events: none;
  position: absolute;
  width: 0;
  overflow: hidden;
}

.is-expanded .top-menu {
  opacity: 1;
  transform: translateX(0);
  position: relative;
  width: auto;
}

.expand-button {
  flex-shrink: 0;
  min-width: fit-content;
  padding: 0.5rem;
  margin-left: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.expand-button:hover {
  transform: scale(1.1);
}

.daisy-dropdown {
  @apply daisy-dropdown-end;
}

@media (max-width: theme('screens.md')) {
  :deep(.label) {
    display: none;
  }
}
</style>

