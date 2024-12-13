<template>
  <div class="sidebar-container daisy-w-full daisy-h-full">
    <div class="daisy-flex daisy-flex-col daisy-h-full">
      <ul v-if="user" class="list-group daisy-flex-1">
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
          <div class="daisy-dropdown">
            <label
              tabindex="0"
              class="daisy-btn daisy-btn-ghost"
              aria-label="User actions"
            >
              <div class="daisy-flex daisy-flex-col daisy-items-center daisy-gap-1">
                <SvgMissingAvatar width="24" height="24" />
                <span class="label">Account</span>
              </div>
            </label>
            <ul tabindex="0" class="daisy-dropdown-content daisy-menu daisy-p-2 daisy-bg-base-100 daisy-rounded-box daisy-w-52 daisy-shadow">
              <li v-if="user?.admin">
                <router-link :to="{ name: 'adminDashboard' }">
                  Admin Dashboard
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'recent' }">
                  <SvgAssimilate class="daisy-mr-2" />Recent...
                </router-link>
              </li>
              <li>
                <PopButton class="daisy-w-full daisy-text-left" title="user settings">
                  <template #button_face>Settings for {{ user.name }}</template>
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
              </li>
              <li>
                <router-link :to="{ name: 'messageCenter' }">
                  Message center
                </router-link>
              </li>
              <li>
                <router-link :to="{ name: 'assessmentAndCertificateHistory' }">
                  My Assessments and Certificates
                </router-link>
              </li>
              <li>
                <a href="#" @click="logout">Logout</a>
              </li>
            </ul>
          </div>
        </li>
      </ul>
      <LoginButton v-else />
      <div class="daisy-flex daisy-flex-col daisy-items-center">
        <router-link
          to="/"
          class="brand-text [writing-mode:vertical-lr] daisy-text-center daisy-py-4 daisy-font-bold daisy-text-neutral-400 daisy-whitespace-nowrap daisy-no-underline"
        >
          Doughnut by
        </router-link>
        <a href="https://odd-e.com" target="_blank" class="daisy-mb-4">
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

  .label {
    font-size: 0.8rem;
    line-height: 1;
  }
}

@media (max-width: theme('screens.lg')) {
  .sidebar-container {
    height: auto;
    display: block;

    .list-group {
      flex-direction: row;
      flex-wrap: nowrap;
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

  .brand-text,
  a[href="https://odd-e.com"] {
    display: none;
  }
}

@media (max-width: theme('screens.md')) {
  :deep(.label) {
    display: none;
  }

  .daisy-dropdown {
    @apply daisy-dropdown-end;
  }
}

</style>
