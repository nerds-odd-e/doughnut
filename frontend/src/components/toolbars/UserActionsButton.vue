<template>
  <div class="dropup w-100">
    <a
      aria-label="User actions"
      data-bs-toggle="dropdown"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
    >
      <slot />
    </a>
    <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
      <router-link
        v-if="user?.admin"
        role="button"
        class="dropdown-item"
        :to="{ name: 'adminDashboard' }"
      >
        Admin Dashboard
      </router-link>
      <PopButton btn-class="dropdown-item" title="user settings">
        <template #button_face> Settings for {{ user.name }}</template>
        <!-- prettier-ignore -->
        <template #default="{ closer }">
          <UserProfileDialog
            v-bind="{ user }"
            @user-updated="
              if ($event) {
                $emit('updateUser', $event);
              }
              closer();
            "
          />
        </template>
      </PopButton>
      <router-link role="button" class="dropdown-item" :to="{ name: 'messageCenter' }">Message center</router-link>
      <router-link role="button" class="dropdown-item" :to="{ name: 'assessmentAndCertificateHistory' }"> My Assessments and Certificates </router-link>
      <a href="#" class="dropdown-item" role="button" @click="logout">Logout</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import PopButton from "@/components/commons/Popups/PopButton.vue"
import type { User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"
import UserProfileDialog from "./UserProfileDialog.vue"

const { managedApi } = useLoadingApi()
defineProps({
  user: { type: Object as PropType<User>, required: true },
})

defineEmits(["updateUser"])

const logout = async () => {
  await managedApi.logout()
  window.location.href = "/d/bazaar"
}
</script>
