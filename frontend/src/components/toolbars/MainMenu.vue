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
import type { User } from "@generated/donut-backend-api"
import type { PropType } from "vue"
import { UserController } from "@generated/donut-backend-api/sdk.gen"
import { watch, computed } from "vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRecallData } from "@/composables/useRecallData"
import { useNavigationItems } from "@/composables/useNavigationItems"
import { messageCenter } from "@/store/messageCenter"
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
const {
  toRepeat,
  currentRecallWindowEndAt,
  setToRepeat,
  setDueCommissioned,
  setCurrentRecallWindowEndAt,
  setTotalAssimilatedCount,
} = useRecallData()

const fetchMenuData = async () => {
  const { data: menuData, error } = await UserController.getMenuData({
    query: { timezone: timezoneParam() },
  })
  if (!error && menuData) {
    applyAssimilationCountDto(menuData.assimilationCount)
    if (menuData.recallStatus) {
      if ((toRepeat.value?.length ?? 0) === 0) {
        setToRepeat(menuData.recallStatus.toRepeat)
      }
      setDueCommissioned(menuData.recallStatus.dueCommissioned)
      if (!currentRecallWindowEndAt.value) {
        setCurrentRecallWindowEndAt(
          menuData.recallStatus.currentRecallWindowEndAt
        )
      }
      setTotalAssimilatedCount(menuData.recallStatus.totalAssimilatedCount)
    }
    if (menuData.unreadMessages !== undefined) {
      messageCenter.unreadMessages = menuData.unreadMessages
    }
  }
}

const fetchMenuDataIfLoggedIn = () => {
  if (props.user) {
    fetchMenuData()
  }
}

watch(() => props.user, fetchMenuDataIfLoggedIn, { immediate: true })

watch(
  () => route.name,
  (name, previousName) => {
    if (previousName && name !== previousName) {
      fetchMenuDataIfLoggedIn()
    }
  }
)

const logout = async () => {
  await fetch("/logout", {
    method: "POST",
  })
  window.location.href = "/bazaar"
}
</script>
