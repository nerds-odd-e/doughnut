<template>
  <div class="sidebar-container daisy-w-full daisy-h-full">
    <div
      ref="menuRef"
      tabindex="0"
      class="menu-wrapper daisy-flex daisy-flex-col daisy-h-full daisy-bg-neutral"
      :class="{ 'is-expanded': shouldShowExpanded, 'is-collapsed': !shouldShowExpanded }"
      @blur="handleFocusLoss"
    >
      <div
        class="menu-content daisy-flex daisy-flex-row daisy-items-center"
        :class="{ 'clickable-when-collapsed': !shouldShowExpanded && user }"
        @click="handleMenuContentClick"
      >
        <!-- Login button: always visible when no user -->
        <LoginButton v-if="!user" />

        <!-- User menu content -->
        <template v-if="user">
          <!-- Collapsed state: show only active item (not on home page) -->
          <ul v-if="!shouldShowExpanded && hasActiveItem && !isHomePage && activeItem" class="collapsed-menu daisy-menu">
            <li class="daisy-menu-item active-item-only">
              <div @click.capture.stop.prevent="handleActiveItemClick" class="active-item-wrapper">
                <NavigationItem v-bind="{ ...activeItem }" @resumeRecall="resumeRecall" />
              </div>
            </li>
          </ul>

          <!-- Expanded state: show all items -->
          <ul v-if="shouldShowExpanded" class="top-menu daisy-menu daisy-flex-1">
        <template v-if="!isHomePage">
          <li v-for="item in upperNavItems" :title="item.label" :key="item.name" class="daisy-menu-item">
            <NavigationItem v-bind="{ ...item }" @resumeRecall="resumeRecall" />
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
      </div>

      <!-- Expand button: only visible when user is logged in, always aligned to right -->
      <button
        v-if="user"
        class="expand-button daisy-btn daisy-btn-ghost daisy-btn-sm"
        :class="{ 'is-expanded': shouldShowExpanded }"
        @click.stop="toggleExpanded"
        aria-label="Toggle menu"
      >
        <SvgChevronRight />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { User } from "@generated/backend"
import type { PropType, Component, Ref } from "vue"
import { ref, computed, onMounted, onUnmounted, watch } from "vue"
import { useRoute } from "vue-router"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import SvgChevronRight from "@/components/svgs/SvgChevronRight.vue"
import { useRecallData } from "@/composables/useRecallData"

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

// When no user, always show expanded menu (just login button)
const shouldShowExpanded = computed(() => {
  return !props.user || isExpanded.value
})

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

const expandMenu = () => {
  isExpanded.value = true
}

const collapseMenu = () => {
  isExpanded.value = false
}

const handleMenuContentClick = () => {
  // When collapsed and user is logged in, clicking anywhere expands the menu
  if (!shouldShowExpanded.value && props.user) {
    expandMenu()
  }
}

const { resumeRecall } = useRecallData()

const handleActiveItemClick = (event: MouseEvent) => {
  // When collapsed, clicking the active item should expand the menu, not navigate
  // Exception: "resumeRecall" should resume without expanding
  if (!shouldShowExpanded.value && props.user) {
    if (activeItem.value?.name === "resumeRecall") {
      event.preventDefault()
      event.stopPropagation()
      resumeRecall()
      return
    }
    event.preventDefault()
    event.stopPropagation()
    expandMenu()
  }
}

const isClickWithinAccountDropdown = (target: Node | null): boolean => {
  if (!target) return false
  let element = target as HTMLElement | null
  while (element) {
    // Check if the element is within a details dropdown (account menu uses details element)
    if (element.tagName === "DETAILS" && element.closest(".menu-wrapper")) {
      return true
    }
    // Check if the element is within the dropdown content
    if (element.classList?.contains("daisy-dropdown-content")) {
      return true
    }
    // Check if the element is the account menu item or within it
    if (element.getAttribute?.("aria-label") === "Account") {
      return true
    }
    // Check if parent is the account menu item
    const accountMenuItem = element.closest?.('li[class*="menu-item"]')
    if (accountMenuItem) {
      const navItem = accountMenuItem.querySelector('[aria-label="Account"]')
      if (navItem) {
        return true
      }
    }
    element = element.parentElement
  }
  return false
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as Node
  // Don't collapse if clicking on account dropdown or its content
  if (isClickWithinAccountDropdown(target)) {
    return
  }
  if (menuRef.value && !menuRef.value.contains(target)) {
    collapseMenu()
  }
}

const handleFocusLoss = () => {
  // Use setTimeout to allow click events to process first
  setTimeout(() => {
    const activeElement = document.activeElement
    // Don't collapse if focus is within the account dropdown
    if (activeElement && isClickWithinAccountDropdown(activeElement)) {
      return
    }
    if (menuRef.value && activeElement !== menuRef.value) {
      // Check if active element is still within the menu or its dropdowns
      if (activeElement && menuRef.value.contains(activeElement)) {
        return
      }
      collapseMenu()
    }
  }, 0)
}

const route = useRoute()

// Watch for route changes and collapse menu
watch(
  () => route.fullPath,
  () => {
    collapseMenu()
  }
)

onMounted(() => {
  document.addEventListener("click", handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener("click", handleClickOutside)
})
</script>

<style lang="scss" scoped>
@use "@/assets/menu-variables.scss" as *;

.sidebar-container {
  height: auto;
  display: block;
  width: auto; // Not full width, only as wide as content
  background-color: transparent; // Make transparent so menu-wrapper background shows
}

.menu-wrapper {
  position: relative;
  outline: none;
  display: flex;
  align-items: center;
  width: 100%;
  border-top-right-radius: 1rem;
  border-bottom-right-radius: 1rem;
  margin-right: 0.5rem; // Add space on the right to show rounded border
  overflow: visible; // Allow dropdowns to overflow
  // Background color comes from daisy-bg-neutral class
  flex-direction: row; // Ensure horizontal layout
  flex-wrap: nowrap; // Prevent wrapping
}

// Create a pseudo-element for the background with rounded corners
.menu-wrapper::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: hsl(var(--n)); // Match the neutral background
  border-top-right-radius: 1rem;
  border-bottom-right-radius: 1rem;
  z-index: -1; // Behind the content
  pointer-events: none; // Don't block interactions
}

.menu-content {
  flex: 1; // Take available space but allow chevron to fit
  min-width: 0; // Allow shrinking
  transition: all 0.3s ease;
  align-items: center;
  min-height: 100%;
  display: flex;
}

.menu-wrapper.is-collapsed {
  width: auto;
  min-width: $collapsed-menu-width-tablet;
}

@media (max-width: theme('screens.md')) {
  .menu-wrapper.is-collapsed {
    min-width: $collapsed-menu-width-mobile;
  }
}

.is-collapsed .menu-content {
  flex: 1; // Take full remaining width when collapsed
  width: auto;
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

  :deep(.nav-item) {
    width: 100%;
    display: flex;
    justify-content: center;
  }

  :deep(.nav-item > a),
  :deep(.nav-item > details > summary) {
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
  }

  .label {
    font-size: 0.8rem;
    line-height: 1;
  }
}

.active-item-only {
  flex: 1; // Take full width
  min-width: 0; // Allow shrinking
  display: flex;
  align-items: center;
}

.active-item-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0;
}

.top-menu,
.collapsed-menu {
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 1rem;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.collapsed-menu {
  opacity: 1;
  transform: translateX(0);
  position: relative;
  width: 100%;
  padding-right: 0;
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
  height: 100%;
  padding: 0 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  border: none;
  background: transparent;
  transition: background-color 0.2s ease, transform 0.3s ease;
  margin-left: auto; // Push to the rightmost position

  :deep(svg) {
    transition: transform 0.3s ease;
  }
}

.expand-button.is-expanded {
  :deep(svg) {
    transform: rotate(180deg);
  }
}

.expand-button:hover {
  background-color: hsl(var(--bc) / 0.1);
}

.expand-button:active {
  background-color: hsl(var(--bc) / 0.15);
}

.clickable-when-collapsed {
  cursor: pointer;
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

