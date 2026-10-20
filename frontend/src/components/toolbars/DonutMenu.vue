<template>
  <AutoCollapseDropdown
    ref="dropdownRef"
    v-slot="{ open }"
    class="donut-menu daisy-dropdown daisy-dropdown-bottom daisy-dropdown-start flex-none"
  >
    <summary
      role="button"
      aria-label="Donut menu"
      title="Donut menu"
      class="daisy-btn daisy-btn-ghost list-none cursor-pointer"
    >
      <BookMarked class="w-6 h-6" aria-hidden="true" />
    </summary>
    <div
      v-if="open"
      role="region"
      aria-label="Donut menu panel"
      class="daisy-dropdown-content z-50 bg-neutral rounded-box shadow-lg p-2"
    >
      <ul v-if="user" class="grid grid-cols-4 gap-2">
        <li
          v-for="item in [...upperNavItems, ...lowerNavItems]"
          :key="item.name"
          :title="item.label"
          @click="close"
        >
          <NavigationItem
            v-bind="item"
            @resumeRecall="resumeRecall"
            @goToNextAssimilation="goToNextAssimilation"
          />
        </li>
        <AccountMenuItem
          :user="user"
          :logout="chooseLogout"
        />
      </ul>
      <LoginButton v-else />
      <div class="flex items-center justify-end gap-2 pt-2 text-sm">
        <router-link to="/" class="text-neutral-400 no-underline">
          Donut by
        </router-link>
        <a href="https://odd-e.com" target="_blank">
          <img src="/odd-e.png" width="24" height="24" alt="" />
        </a>
      </div>
    </div>
  </AutoCollapseDropdown>
</template>

<script setup lang="ts">
import type { User } from "@generated/donut-backend-api"
import { onMounted, onUnmounted, ref, watch } from "vue"
import { useRoute } from "vue-router"
import { BookMarked } from "@lucide/vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import { useNavigationItems } from "@/composables/useNavigationItems"
import { useResumeRecall } from "@/composables/useResumeRecall"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { logout } from "@/managedApi/window/logout"

defineProps<{ user?: User }>()

const dropdownRef = ref<InstanceType<typeof AutoCollapseDropdown> | null>(null)
const close = () => dropdownRef.value?.closeDropdown()

const { upperNavItems, lowerNavItems } = useNavigationItems()
const { resumeRecall } = useResumeRecall()
const { goToNextAssimilation } = useGoToNextAssimilation()

const chooseLogout = () => {
  logout()
  close()
}

const route = useRoute()
watch(() => route.fullPath, close)

const closeOnEscape = (event: KeyboardEvent) => {
  if (event.key === "Escape") close()
}
onMounted(() => document.addEventListener("keydown", closeOnEscape))
onUnmounted(() => document.removeEventListener("keydown", closeOnEscape))
</script>

<style scoped lang="scss">
.donut-menu > .daisy-dropdown-content {
  width: max-content;
  max-width: calc(100vw - 2rem);
}

@media (max-width: 640px) {
  .donut-menu {
    position: static;
  }

  .donut-menu > .daisy-dropdown-content {
    left: 0;
    right: 0;
    width: auto;
    max-width: none;
    border-radius: 0;
  }
}

summary::marker {
  display: none;
}

summary::-webkit-details-marker {
  display: none;
}
</style>
