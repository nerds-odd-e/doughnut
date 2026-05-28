<template>
  <li class="daisy-menu-item">
    <NavigationItem
      label="Account"
      :icon="UserIcon"
      :has-dropdown="true"
      :is-active="false"
    >
      <template #dropdown="slotProps">
        <DropdownMenu panel-class="max-w-52 overflow-hidden">
          <li v-if="user?.admin" class="daisy-menu-item hover:bg-base-200">
            <router-link :to="{ name: 'adminDashboard' }" class="daisy-menu-title justify-start text-primary hover:text-primary-focus w-full text-left truncate" @click="slotProps.closeDropdown">
              Admin Dashboard
            </router-link>
          </li>
          <li class="daisy-menu-item hover:bg-base-200">
            <router-link :to="{ name: 'recent' }" class="daisy-menu-title justify-start text-primary hover:text-primary-focus w-full text-left truncate" @click="slotProps.closeDropdown">
              <CircleCheck class="mr-2 w-6 h-6 shrink-0" />Recent...
            </router-link>
          </li>
          <li class="daisy-menu-item hover:bg-base-200">
            <a href="#" class="daisy-menu-title justify-start text-primary hover:text-primary-focus w-full text-left truncate" @click="(e) => { e.preventDefault(); showUserSettingsDialog(); slotProps.closeDropdown(); }">
              Settings for {{ user.name }}
            </a>
          </li>
          <li class="daisy-menu-item hover:bg-base-200">
            <router-link :to="{ name: 'manageAccessTokens' }" class="daisy-menu-title justify-start text-primary hover:text-primary-focus w-full text-left truncate" @click="slotProps.closeDropdown">
              Manage Access Tokens
            </router-link>
          </li>
          <li class="daisy-menu-item hover:bg-base-200">
            <a href="#" class="daisy-menu-title justify-start text-primary hover:text-primary-focus w-full text-left truncate" @click="(e) => { e.preventDefault(); logout(); slotProps.closeDropdown(); }">
              Logout
            </a>
          </li>
        </DropdownMenu>
      </template>
    </NavigationItem>
  </li>
</template>

<script setup lang="ts">
import type { User } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import { CircleCheck, User as UserIcon } from "@lucide/vue"

defineProps({
  user: { type: Object as PropType<User>, required: true },
  showUserSettingsDialog: {
    type: Function as PropType<() => void>,
    required: true,
  },
  logout: { type: Function as PropType<() => void>, required: true },
})
</script>

