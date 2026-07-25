<template>
  <li class="daisy-menu-item">
    <NavigationItem
      label="Account"
      :icon="UserIcon"
      :has-dropdown="true"
      :is-active="false"
    >
      <template #dropdown="{ closeDropdown }">
        <DropdownMenu>
          <DropdownMenuItem v-if="user?.admin">
            <router-link
              :to="{ name: 'adminDashboard' }"
              :class="dropdownMenuButtonClass"
              @click="closeDropdown"
            >
              Admin Dashboard
            </router-link>
          </DropdownMenuItem>
          <DropdownMenuItem>
            <router-link
              :to="{ name: 'settingsRecent' }"
              :class="dropdownMenuButtonClass"
              @click="closeDropdown"
            >
              <CircleCheck class="shrink-0" :size="20" aria-hidden="true" />
              <span>Recent...</span>
            </router-link>
          </DropdownMenuItem>
          <DropdownMenuItem>
            <router-link
              :to="{ name: 'settingsGeneral' }"
              :class="dropdownMenuButtonClass"
              @click="closeDropdown"
            >
              {{ settingsTitle }}
            </router-link>
          </DropdownMenuItem>
          <DropdownMenuItem>
            <router-link
              :to="{ name: 'settingsAccessTokens' }"
              :class="dropdownMenuButtonClass"
              @click="closeDropdown"
            >
              Manage Access Tokens
            </router-link>
          </DropdownMenuItem>
          <DropdownMenuItem>
            <DropdownMenuActionButton
              title="Logout"
              @click="onLogout(closeDropdown)"
            />
          </DropdownMenuItem>
        </DropdownMenu>
      </template>
    </NavigationItem>
  </li>
</template>

<script setup lang="ts">
import type { User } from "@generated/doughnut-backend-api"
import { computed, type PropType } from "vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"
import { CircleCheck, User as UserIcon } from "@lucide/vue"

const props = defineProps({
  user: { type: Object as PropType<User>, required: true },
  logout: { type: Function as PropType<() => void>, required: true },
})

const settingsTitle = computed(() => `Settings for ${props.user.name}`)

const onLogout = (closeDropdown: () => void) => {
  props.logout()
  closeDropdown()
}
</script>
