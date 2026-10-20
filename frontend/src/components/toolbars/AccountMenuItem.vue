<template>
  <li class="daisy-menu-item">
    <NavigationItem
      label="Account"
      :icon="UserIcon"
      :expanded="expanded"
      :is-active="false"
      @toggle="expanded = !expanded"
    />
  </li>
  <li v-if="expanded" class="col-span-4">
    <ul
      class="daisy-menu w-full p-0 text-neutral-content"
      @click="emit('choose')"
    >
      <li v-if="user?.admin" :class="dropdownMenuItemClass">
        <router-link
          :to="{ name: 'adminDashboard' }"
          :class="dropdownMenuButtonClass"
        >
          Admin Dashboard
        </router-link>
      </li>
      <li :class="dropdownMenuItemClass">
        <router-link
          :to="{ name: 'settingsRecent' }"
          :class="dropdownMenuButtonClass"
        >
          <CircleCheck class="shrink-0" :size="20" aria-hidden="true" />
          <span>Recent...</span>
        </router-link>
      </li>
      <li :class="dropdownMenuItemClass">
        <router-link
          :to="{ name: 'settingsGeneral' }"
          :class="dropdownMenuButtonClass"
        >
          {{ settingsTitle }}
        </router-link>
      </li>
      <li :class="dropdownMenuItemClass">
        <router-link
          :to="{ name: 'settingsAccessTokens' }"
          :class="dropdownMenuButtonClass"
        >
          Manage Access Tokens
        </router-link>
      </li>
      <li :class="dropdownMenuItemClass">
        <DropdownMenuActionButton title="Logout" @click="logout" />
      </li>
    </ul>
  </li>
</template>

<script setup lang="ts">
import type { User } from "@generated/donut-backend-api"
import { computed, type PropType, ref } from "vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import {
  dropdownMenuButtonClass,
  dropdownMenuItemClass,
} from "@/components/commons/dropdownMenuClasses"
import { CircleCheck, User as UserIcon } from "@lucide/vue"

const props = defineProps({
  user: { type: Object as PropType<User>, required: true },
  logout: { type: Function as PropType<() => void>, required: true },
})

const emit = defineEmits<{ (e: "choose"): void }>()

const expanded = ref(false)

const settingsTitle = computed(() => `Settings for ${props.user.name}`)
</script>
