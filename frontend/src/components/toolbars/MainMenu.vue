<template>
  <VerticalMenu
    v-if="isLgOrLarger"
    :user="user"
    :upper-nav-items="upperNavItems"
    :lower-nav-items="lowerNavItems"
    :is-home-page="isHomePage"
    :show-user-settings-dialog="showUserSettingsDialog"
    :logout="logout"
  />
  <HorizontalMenu
    v-else
    :user="user"
    :upper-nav-items="upperNavItems"
    :lower-nav-items="lowerNavItems"
    :is-home-page="isHomePage"
    :show-user-settings-dialog="showUserSettingsDialog"
    :logout="logout"
  />
  <Modal v-if="showUserSettings" @close_request="showUserSettings = false">
    <template #body>
      <UserProfileDialog
        v-bind="{ user }"
        @user-updated="
          if ($event) {
            $emit('updateUser', $event);
          }
          showUserSettings = false;
        "
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { User } from "@generated/backend"
import type { PropType } from "vue"
import UserProfileDialog from "./UserProfileDialog.vue"
import { UserController } from "@generated/backend/sdk.gen"
import { watch, computed, ref } from "vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRecallData } from "@/composables/useRecallData"
import { useNavigationItems } from "@/composables/useNavigationItems"
import { messageCenterConversations } from "@/store/messageStore"
import Modal from "@/components/commons/Modal.vue"
import { useBreakpoint } from "@/composables/useBreakpoint"
import { useRoute } from "vue-router"
import VerticalMenu from "@/components/toolbars/VerticalMenu.vue"
import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"

const props = defineProps({
  user: { type: Object as PropType<User>, required: false },
})

defineEmits<{
  (e: "updateUser", user: User): void
}>()

const route = useRoute()
const { isLgOrLarger } = useBreakpoint()
const { upperNavItems, lowerNavItems } = useNavigationItems()
const isHomePage = computed(() => route.name === "home")
const showUserSettings = ref(false)

const { setDueCount, setAssimilatedCountOfTheDay, setTotalUnassimilatedCount } =
  useAssimilationCount()
const { setToRepeat, setRecallWindowEndAt, setTotalAssimilatedCount } =
  useRecallData()

const fetchMenuData = async () => {
  const { data: menuData, error } = await UserController.getMenuData({
    query: { timezone: timezoneParam() },
  })
  if (!error && menuData) {
    if (menuData.assimilationCount) {
      setDueCount(menuData.assimilationCount.dueCount)
      setAssimilatedCountOfTheDay(
        menuData.assimilationCount.assimilatedCountOfTheDay
      )
      setTotalUnassimilatedCount(
        menuData.assimilationCount.totalUnassimilatedCount
      )
    }
    if (menuData.recallStatus) {
      setToRepeat(menuData.recallStatus.toRepeat)
      setRecallWindowEndAt(menuData.recallStatus.recallWindowEndAt)
      setTotalAssimilatedCount(menuData.recallStatus.totalAssimilatedCount)
    }
    if (menuData.unreadConversations !== undefined) {
      messageCenterConversations.unreadConversations =
        menuData.unreadConversations
    }
  }
}

watch(
  () => props.user,
  () => {
    if (props.user) {
      fetchMenuData()
    }
  },
  { immediate: true }
)

const logout = async () => {
  await fetch("/logout", {
    method: "POST",
  })
  window.location.href = "/d/bazaar"
}

const showUserSettingsDialog = () => {
  showUserSettings.value = true
}
</script>
