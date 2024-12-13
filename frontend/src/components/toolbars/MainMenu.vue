<template>
  <div v-if="user" class="sidebar-container daisy-w-full daisy-h-full">
    <div class="daisy-flex daisy-flex-col daisy-h-full">
      <ul class="list-group daisy-flex-1">
        <template v-if="!isHomePage">
          <li v-for="item in upperNavItems" role="button" :title="item.label" :key="item.name" class="list-item">
            <NavigationItem v-bind="item" />
          </li>
        </template>

        <template v-if="!isHomePage">
          <li v-for="item in lowerNavItems" role="button" :title="item.label" :key="item.name" class="list-item">
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
      <div class="daisy-flex daisy-flex-col daisy-items-center">
        <router-link to="/" class="vertical-text">Doughnut by</router-link>
        <a href="https://odd-e.com" target="_blank" class="daisy-mb-4">
          <img
            src="/odd-e.png"
            width="35"
            height="35"
            class="d-inline-block align-top"
            alt=""
          />
        </a>
      </div>
    </div>
  </div>
  <LoginButton v-else />
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
.list-group {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.list-item {
  border: none;
  padding: 0.5rem;
  background: none;
  text-align: center;
  width: 100%;
  display: flex;
  justify-content: center;

  .menu-label {
    font-size: 0.7rem;
    line-height: 1;
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
  text-decoration: none;
}

@media (max-width: theme('screens.lg')) {
  .sidebar-container {
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
      width: auto;
      border: none;
      padding: 0.5rem;
      background: none;
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

@media (max-width: theme('screens.md')) {
  .menu-label {
    display: none;
  }
}

</style>
