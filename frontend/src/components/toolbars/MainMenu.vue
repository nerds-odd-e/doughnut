<template>
  <div class="sidebar-container daisy:w-full daisy:h-full">
    <div class="daisy:flex daisy:flex-col daisy:h-full">
      <ul v-if="user" class="daisy:menu daisy:w-full daisy:flex-1">
        <template v-if="!isHomePage">
          <li v-for="item in upperNavItems" role="button" :title="item.label" :key="item.name" class="daisy-menu-item">
            <NavigationItem v-bind="{ ...item }" />
          </li>
        </template>

        <template v-if="!isHomePage">
          <li v-for="item in lowerNavItems" role="button" :title="item.label" :key="item.name" class="daisy-menu-item">
            <NavigationItem v-bind="{ ...item, to: item.name }" />
          </li>
        </template>

        <li class="daisy-menu-item">
          <NavigationItem
            label="Account"
            :icon="SvgMissingAvatar"
            :has-dropdown="true"
            :is-active="false"
          >
            <template #dropdown="slotProps">
              <ul tabindex="0" class="daisy-dropdown-content daisy:p-2 daisy:bg-base-100 daisy:rounded-box daisy:w-52 daisy:shadow">
                <li v-if="user?.admin">
                  <router-link :to="{ name: 'adminDashboard' }" @click="slotProps.closeDropdown">
                    Admin Dashboard
                  </router-link>
                </li>
                <li>
                  <router-link :to="{ name: 'recent' }" @click="slotProps.closeDropdown">
                    <SvgAssimilate class="daisy:mr-2" />Recent...
                  </router-link>
                </li>
                <li>
                  <div
                    class="daisy:w-full daisy:text-left"
                    @click="slotProps.closeDropdown"
                  >
                    <PopButton title="user settings">
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
                  </div>
                </li>
                <li>
                  <router-link :to="{ name: 'assessmentAndCertificateHistory' }" @click="slotProps.closeDropdown">
                    My Assessments and Certificates
                  </router-link>
                </li>
                <li>
                  <a href="#" @click="(e) => { e.preventDefault(); logout(); slotProps.closeDropdown(); }">
                    Logout
                  </a>
                </li>
              </ul>
            </template>
          </NavigationItem>
        </li>
      </ul>
      <LoginButton v-else />
      <div class="daisy:flex daisy:flex-col daisy:items-center">
        <router-link
          to="/"
          class="brand-text daisy:[writing-mode:vertical-lr] daisy:text-center daisy:py-4 daisy:font-bold daisy:text-neutral-400 daisy:whitespace-nowrap daisy:no-underline"
        >
          Doughnut by
        </router-link>
        <a href="https://odd-e.com" target="_blank" class="daisy:mb-4">
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
import { watch, computed } from "vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRecallData } from "@/composables/useRecallData"
import { useNavigationItems } from "@/composables/useNavigationItems"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import { messageCenterConversations } from "@/store/messageStore"

const props = defineProps({
  user: { type: Object as PropType<User>, required: false },
})

defineEmits<{
  (e: "updateUser", user: User): void
}>()

const route = useRoute()
const { upperNavItems, lowerNavItems } = useNavigationItems()
const isHomePage = computed(() => route.name === "home")

const { setDueCount, setAssimilatedCountOfTheDay, setTotalUnassimilatedCount } =
  useAssimilationCount()
const { managedApi } = useLoadingApi()
const { setToRepeatCount, setRecallWindowEndAt, setTotalAssimilatedCount } =
  useRecallData()

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
  setTotalAssimilatedCount(overview.totalAssimilatedCount)
}

const fetchUnreadMessageCount = async () => {
  messageCenterConversations.unreadConversations =
    await managedApi.restConversationMessageController.getUnreadConversations()
}

watch(
  () => props.user,
  () => {
    if (props.user) {
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

@media (max-width: theme('screens.lg')) {
  .sidebar-container {
    height: auto;
    display: block;

    .daisy:menu {
      flex-direction: row;
      flex-wrap: nowrap;
      justify-content: center;
      gap: 1rem;
    }

    .daisy-menu-item {
      width: auto;
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
    position: relative;
    display: inline-block;
    right: 0;
    left: auto;
  }
}

</style>
