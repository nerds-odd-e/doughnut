<template>
  <div class="sidebar-container w-full h-full">
    <div class="flex flex-col h-full">
      <ul v-if="user" class="top-menu daisy-menu w-full flex-1">
        <template v-if="!isHomePage">
          <li v-for="item in upperNavItems" :title="item.label" :key="item.name" class="daisy-menu-item">
            <NavigationItem
              v-bind="{ ...item }"
              @resumeRecall="handleResumeRecall"
              @goToNextAssimilation="goToNextAssimilation"
            />
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
      <div class="flex flex-col items-center">
        <router-link
          to="/"
          class="brand-text [writing-mode:vertical-lr] text-center py-4 font-bold text-neutral-400 whitespace-nowrap no-underline"
        >
          Doughnut by
        </router-link>
        <a href="https://odd-e.com" target="_blank" class="mb-4">
          <img
            src="/odd-e.png"
            width="35"
            height="35"
            alt=""
          />
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { User } from "@generated/doughnut-backend-api"
import type { PropType, Component } from "vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import { useRecallData } from "@/composables/useRecallData"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"

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

const { resumeRecall } = useRecallData()
const { goToNextAssimilation } = useGoToNextAssimilation()
const handleResumeRecall = () => {
  resumeRecall()
}
</script>

<style lang="scss" scoped>
.daisy-menu-item {
  padding: 0;
  text-align: center;
  width: 100%;
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
</style>

