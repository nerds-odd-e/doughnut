<template>
  <div class="sidebar-container daisy-w-full daisy-h-full">
    <div class="daisy-flex daisy-flex-col daisy-h-full">
      <ul v-if="user" class="top-menu daisy-menu daisy-w-full daisy-flex-1">
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
      <LoginButton v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { User } from "@generated/backend"
import type { PropType, Component } from "vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"

type NavigationItemType = {
  name?: string
  label: string
  icon: Component
  isActive: boolean
  badge?: number
  badgeClass?: string
  hasDropdown?: boolean
}

defineProps({
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
</script>

<style lang="scss" scoped>
.sidebar-container {
  height: auto;
  display: block;
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

.top-menu {
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 1rem;
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

