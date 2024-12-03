<template>
  <div v-if="user" class="sidebar-container">
    <ul class="list-group">
      <template v-if="!isHomePage">
        <li v-for="item in upperNavItems" role="button" :title="item.label" :key="item.name" class="list-item"
        :class="{ active: item.isActive }">
          <NavigationItem v-bind="item" />
        </li>
      </template>

      <template v-if="!isHomePage">
        <li v-for="item in lowerNavItems" role="button" :title="item.label" :key="item.name" class="list-item"
        :class="{ active: item.isActive }">
          <NavigationItem v-bind="item" />
        </li>
      </template>

      <li role="button" class="list-item" title="User Actions">
        <div class="dropup w-100">
          <a
            aria-label="User actions"
            data-bs-toggle="dropdown"
            data-toggle="dropdown"
            aria-haspopup="true"
            aria-expanded="false"
          >
            <div class="d-flex flex-column align-items-center gap-1">
              <SvgMissingAvatar width="24" height="24" />
              <span class="menu-label">Account</span>
            </div>
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
            <router-link role="button" class="dropdown-item" :to="{ name: 'recent' }">
              <SvgAssimilate class="me-2" />Recent...
            </router-link>
            <PopButton btn-class="dropdown-item" title="user settings">
              <template #button_face> Settings for {{ user.name }}</template>
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
            <router-link role="button" class="dropdown-item" :to="{ name: 'assessmentAndCertificateHistory' }">My Assessments and Certificates</router-link>
            <a href="#" class="dropdown-item" role="button" @click="logout">Logout</a>
          </div>
        </div>
      </li>
    </ul>
  </div>
  <LoginButton v-else />
  <router-link to="/" class="vertical-text">Doughnut by</router-link>
  <a href="https://odd-e.com" target="_blank">
    <img
      src="/odd-e.png"
      width="35"
      height="35"
      class="d-inline-block align-top"
      alt=""
    />
  </a>
</template>

<script setup lang="ts">
import type { User } from "@/generated/backend"
import type { PropType } from "vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import { useRoute } from "vue-router"
import SvgAssimilate from "@/components/svgs/SvgAssimilate.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import UserProfileDialog from "./UserProfileDialog.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { watch } from "vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRecallData } from "@/composables/useRecallData"
import { computed } from "vue"
import { useNavigationItems } from "@/composables/useNavigationItems"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import { messageCenterConversations } from "@/store/messageStore"

const { user } = defineProps({
  user: { type: Object as PropType<User> },
})
defineEmits(["updateUser"])

const route = useRoute()
const { upperNavItems, lowerNavItems } = useNavigationItems()

const isHomePage = computed(() => route.name === "home")

const { setDueCount, setAssimilatedCountOfTheDay, setTotalUnassimilatedCount } =
  useAssimilationCount()
const { managedApi } = useLoadingApi()
const { setToRepeatCount, setRecallWindowEndAt } = useRecallData()

const fetchDueCount = async () => {
  const count = await managedApi.assimilationController.getAssimilationCount(
    timezoneParam()
  )
  setDueCount(count.dueCount)
  setAssimilatedCountOfTheDay(count.assimilatedCountOfTheDay)
  setTotalUnassimilatedCount(count.totalUnassimilatedCount)
}

const fetchRecallCount = async () => {
  const overview = await managedApi.restRecallsController.overview(
    timezoneParam()
  )
  setToRepeatCount(overview.toRepeatCount)
  setRecallWindowEndAt(overview.recallWindowEndAt)
}

const fetchUnreadMessageCount = async () => {
  messageCenterConversations.unreadConversations =
    await managedApi.restConversationMessageController.getUnreadConversations()
}

watch(
  () => user,
  () => {
    if (user) {
      fetchDueCount()
      fetchRecallCount()
      fetchUnreadMessageCount()
    }
  },
  { immediate: true }
)

const logout = async () => {
  await managedApi.logout()
  window.location.href = "/d/bazaar"
}
</script>

<style lang="scss" scoped>
@import '@/styles/_variables.scss';

.sidebar-container {
  display: flex;
  flex-direction: column;
  height: auto;
  overflow: hidden;
  width: 100%;

  :deep(.dropup) {
    position: static;

    .dropdown-menu {
      position: fixed;
      bottom: 60px;
      left: 0;
      z-index: 10000;
    }
  }
}

.list-group {
  list-style: none;
  padding: 0;
  margin: 0;
}

.list-item {
  border: none;
  padding: 0.5rem;
  background: none;
  text-align: center;

  .menu-label {
    font-size: 0.7rem;
    line-height: 1;
  }

  :deep(a) {
    text-decoration: none;
    color: inherit;
  }

  &.active {
    background-color: #404040;

    :deep(a) {
      color: #66b0ff;
    }
  }
}

.vertical-text {
  writing-mode: vertical-lr;
  transform: none;
  text-align: center;
  padding: 1rem 0;
  font-weight: bold;
  color: #a0a0a0;
  white-space: nowrap;
  margin-top: 1rem;
  align-self: center;
  text-decoration: none;
}

a[href="https://odd-e.com"] {
  align-self: center;
  margin-bottom: 1rem;
}

@media (max-width: $tablet-breakpoint) {
  .sidebar-container {
    width: 100%;
    height: auto;
    display: block;

    .list-group {
      flex-direction: row;
      flex-wrap: nowrap;
      overflow-x: auto;
      justify-content: center;
      gap: 1rem;
    }

    .list-item {
      border: none;
      padding: 0.5rem;
      background: none;
    }

    .admin-dashboard {
      display: none;
    }

    .fixed-bottom-bar {
      display: none;
    }
  }

  .vertical-text,
  a[href="https://odd-e.com"] {
    display: none;
  }

  .list-item {
    .menu-label {
      font-size: 0.65rem;
    }
  }
}

@media (max-width: $mobile-breakpoint) {
  .menu-label {
    display: none;
  }
}

</style>
