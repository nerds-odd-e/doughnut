<template>
  <VerticalMenu
    v-if="isLgOrLarger"
    :user="user"
    :upper-nav-items="upperNavItems"
    :lower-nav-items="lowerNavItems"
    :is-home-page="isHomePage"
    :logout="logout"
  />
  <HorizontalMenu
    v-else
    :user="user"
    :upper-nav-items="upperNavItems"
    :lower-nav-items="lowerNavItems"
    :is-home-page="isHomePage"
    :logout="logout"
  />
</template>

<script setup lang="ts">
import type { User } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { watch, computed } from "vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRecallData } from "@/composables/useRecallData"
import { useNavigationItems } from "@/composables/useNavigationItems"
import { messageCenterConversations } from "@/store/messageStore"
import { useBreakpoint } from "@/composables/useBreakpoint"
import { useRoute } from "vue-router"
import VerticalMenu from "@/components/toolbars/VerticalMenu.vue"
import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"

const props = defineProps({
  user: { type: Object as PropType<User>, required: false },
})

const route = useRoute()
const { isLgOrLarger } = useBreakpoint()
const { upperNavItems, lowerNavItems } = useNavigationItems()
const isHomePage = computed(() => route.name === "home")

const { applyAssimilationCountDto } = useAssimilationCount()
const { setToRepeat, setCurrentRecallWindowEndAt, setTotalAssimilatedCount } =
  useRecallData()

const fetchMenuData = async () => {
  const { data: menuData, error } = await UserController.getMenuData({
    query: { timezone: timezoneParam() },
  })
  if (!error && menuData) {
    applyAssimilationCountDto(menuData.assimilationCount)
    if (menuData.recallStatus) {
      setToRepeat(menuData.recallStatus.toRepeat)
      setCurrentRecallWindowEndAt(
        menuData.recallStatus.currentRecallWindowEndAt
      )
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
  window.location.href = "/bazaar"
}
</script>
