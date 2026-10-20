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
import { computed } from "vue"
import { useNavigationItems } from "@/composables/useNavigationItems"
import { useBreakpoint } from "@/composables/useBreakpoint"
import { logout } from "@/managedApi/window/logout"
import { useRoute } from "vue-router"
import VerticalMenu from "@/components/toolbars/VerticalMenu.vue"
import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"

defineProps({
  user: { type: Object as PropType<User>, required: false },
})

const route = useRoute()
const { isLgOrLarger } = useBreakpoint()
const { upperNavItems, lowerNavItems } = useNavigationItems()
const isHomePage = computed(() => route.name === "home")
</script>
