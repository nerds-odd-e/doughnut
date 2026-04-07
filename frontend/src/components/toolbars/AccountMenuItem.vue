<template>
  <li class="daisy-menu-item">
    <NavigationItem
      label="Account"
      :icon="UserIcon"
      :has-dropdown="true"
      :is-active="false"
    >
      <template #dropdown="slotProps">
        <ul tabindex="0" class="daisy-dropdown-content daisy-menu daisy-p-2 daisy-bg-base-100 daisy-rounded-box daisy-w-52 daisy-shadow daisy-max-w-52 daisy-overflow-hidden daisy-z-[1000]">
          <li v-if="user?.admin" class="daisy-menu-item hover:daisy-bg-base-200">
            <router-link :to="{ name: 'adminDashboard' }" class="daisy-menu-title daisy-justify-start daisy-text-primary hover:daisy-text-primary-focus daisy-w-full daisy-text-left daisy-truncate" @click="slotProps.closeDropdown">
              Admin Dashboard
            </router-link>
          </li>
          <li class="daisy-menu-item hover:daisy-bg-base-200">
            <router-link :to="{ name: 'recent' }" class="daisy-menu-title daisy-justify-start daisy-text-primary hover:daisy-text-primary-focus daisy-w-full daisy-text-left daisy-truncate" @click="slotProps.closeDropdown">
              <CircleCheck class="daisy-mr-2 w-5 h-5 shrink-0" />Recent...
            </router-link>
          </li>
          <li class="daisy-menu-item hover:daisy-bg-base-200">
            <a href="#" class="daisy-menu-title daisy-justify-start daisy-text-primary hover:daisy-text-primary-focus daisy-w-full daisy-text-left daisy-truncate" @click="(e) => { e.preventDefault(); showUserSettingsDialog(); slotProps.closeDropdown(); }">
              Settings for {{ user.name }}
            </a>
          </li>
          <li class="daisy-menu-item hover:daisy-bg-base-200">
            <router-link :to="{ name: 'assessmentAndCertificateHistory' }" class="daisy-menu-title daisy-justify-start daisy-text-primary hover:daisy-text-primary-focus daisy-w-full daisy-text-left daisy-truncate" @click="slotProps.closeDropdown">
              My Assessments and Certificates
            </router-link>
          </li>
          <li class="daisy-menu-item hover:daisy-bg-base-200">
            <router-link :to="{ name: 'manageAccessTokens' }" class="daisy-menu-title daisy-justify-start daisy-text-primary hover:daisy-text-primary-focus daisy-w-full daisy-text-left daisy-truncate" @click="slotProps.closeDropdown">
              Manage Access Tokens
            </router-link>
          </li>
          <li class="daisy-menu-item hover:daisy-bg-base-200">
            <a href="#" class="daisy-menu-title daisy-justify-start daisy-text-primary hover:daisy-text-primary-focus daisy-w-full daisy-text-left daisy-truncate" @click="(e) => { e.preventDefault(); logout(); slotProps.closeDropdown(); }">
              Logout
            </a>
          </li>
        </ul>
      </template>
    </NavigationItem>
  </li>
</template>

<script setup lang="ts">
import type { User } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import { CircleCheck, User as UserIcon } from "lucide-vue-next"

defineProps({
  user: { type: Object as PropType<User>, required: true },
  showUserSettingsDialog: {
    type: Function as PropType<() => void>,
    required: true,
  },
  logout: { type: Function as PropType<() => void>, required: true },
})
</script>

