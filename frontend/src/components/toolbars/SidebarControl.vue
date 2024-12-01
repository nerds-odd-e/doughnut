<template>
  <div v-if="user" class="sidebar-container">
      <ul class="list-group">
        <li role="button" class="list-item" :class="{ active: isActiveRoute(['recalls', 'initial', 'repeat']) }" title="Daily Recall">
          <router-link :to="{ name: 'recalls' }" class="d-flex flex-column align-items-center gap-1">
            <SvgCalendarCheck />
            <span class="menu-label">Recall</span>
          </router-link>
        </li>
        <li role="button" class="list-item" :class="{ active: isActiveRoute(['recent']) }" title="Assimilate">
          <router-link :to="{ name: 'recent' }" class="d-flex flex-column align-items-center gap-1">
            <div class="icon-container">
              <SvgClockHistory />
              <div v-if="dueCount && dueCount > 0" class="due-count">
                {{ dueCount }}
              </div>
            </div>
            <span class="menu-label">Assimilate</span>
          </router-link>
        </li>
        <li role="button" class="list-item" :class="{ active: isActiveRoute(['notebooks', 'noteShow']) }" title="My Notebooks">
          <router-link :to="{ name: 'notebooks' }" class="d-flex flex-column align-items-center gap-1">
            <SvgJournalText />
            <span class="menu-label">Notebooks</span>
          </router-link>
        </li>
        <li role="button" class="list-item" :class="{ active: isActiveRoute(['bazaar']) }" title="Bazaar">
          <router-link :to="{ name: 'bazaar' }" class="d-flex flex-column align-items-center gap-1">
            <SvgShop />
            <span class="menu-label">Bazaar</span>
          </router-link>
        </li>
        <li role="button" class="list-item" :class="{ active: isActiveRoute(['circles', 'circleShow', 'circleJoin']) }" title="Circles">
          <router-link :to="{ name: 'circles' }" class="d-flex flex-column align-items-center gap-1">
            <SvgPeople />
            <span class="menu-label">Circles</span>
          </router-link>
        </li>
        <li role="button" class="list-item" :class="{ active: isActiveRoute(['messageCenter']) }" title="Messages">
          <MessageCenterButton :user="user">
            <SvgChat />
            <template #label>
              <span class="menu-label">Messages</span>
            </template>
          </MessageCenterButton>
        </li>
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
  <span class="vertical-text">Doughnut by</span>
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
import MessageCenterButton from "@/components/toolbars/MessageCenterButton.vue"
import { useRoute } from "vue-router"
import SvgCalendarCheck from "@/components/svgs/SvgCalendarCheck.vue"
import SvgJournalText from "@/components/svgs/SvgJournalText.vue"
import SvgClockHistory from "@/components/svgs/SvgClockHistory.vue"
import SvgShop from "@/components/svgs/SvgShop.vue"
import SvgPeople from "@/components/svgs/SvgPeople.vue"
import SvgChat from "@/components/svgs/SvgChat.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import UserProfileDialog from "./UserProfileDialog.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { ref, watch } from "vue"

const { user } = defineProps({
  user: { type: Object as PropType<User> },
})
defineEmits(["updateUser"])

const route = useRoute()

const isActiveRoute = (routeNames: string[]) => {
  return routeNames.includes(route.name as string)
}

const dueCount = ref<number | undefined>(undefined)
const { managedApi } = useLoadingApi()

const fetchDueCount = async () => {
  const count =
    await managedApi.assimilationController.getOnboardingCount("UTC")
  dueCount.value = count.dueCount
}

watch(
  () => user,
  () => {
    if (user) {
      fetchDueCount()
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

.icon-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.due-count {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #66b0ff;
  color: white;
  border-radius: 50%;
  min-width: 16px;
  height: 16px;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  z-index: 1;
}
</style>
