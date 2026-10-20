<template>
  <div class="flex items-center flex-none">
    <NavigationItem
      v-if="user && activeItem"
      v-bind="activeItem"
      compact
      :name="isNavigationActionItem(activeItem.name) ? activeItem.name : undefined"
      :expanded="dropdownRef?.open"
      @click.stop
      @toggle="dropdownRef?.openDropdown()"
      @resumeRecall="resumeRecall"
      @goToNextAssimilation="goToNextAssimilation"
    />
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
      <component :is="user && activeItem ? ChevronDown : BookMarked" class="w-6 h-6" aria-hidden="true" />
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
        <AccountMenuItem :user="user" :logout="logout" @choose="close" />
      </ul>
      <div v-if="user" class="flex items-center gap-2 pt-2">
        <button
          type="button"
          aria-label="Open search"
          class="search-launcher daisy-input flex-1 min-w-0 bg-base-100"
          @click="openIntoSearch"
        />
        <button
          type="button"
          aria-label="Search"
          title="Search"
          class="search-launcher daisy-btn daisy-btn-square"
          @click="openIntoSearch"
        >
          <Search class="w-6 h-6" aria-hidden="true" />
        </button>
      </div>
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
  </div>
  <Modal
    v-if="searchOpen"
    :show-close-button="false"
    @close_request="closeSearch"
  >
    <template #body>
      <SearchForNoteAndFolder
        :modal-closer="closeSearch"
        @navigate="closeSearch"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { User } from "@generated/donut-backend-api"
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRoute } from "vue-router"
import { BookMarked, ChevronDown, Search } from "@lucide/vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import Modal from "@/components/commons/Modal.vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import { isNavigationActionItem } from "@/components/navigation/navigationActionItems"
import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import SearchForNoteAndFolder from "@/components/search/SearchForNoteAndFolder.vue"
import { useNavigationItems } from "@/composables/useNavigationItems"
import { useResumeRecall } from "@/composables/useResumeRecall"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { useKeyboardShortcut } from "@/composables/useKeyboardShortcut"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import { logout } from "@/managedApi/window/logout"

const props = defineProps<{ user?: User }>()

const dropdownRef = ref<InstanceType<typeof AutoCollapseDropdown> | null>(null)
const close = () => dropdownRef.value?.closeDropdown()

const searchOpen = ref(false)
const closeSearch = () => {
  searchOpen.value = false
}
const openIntoSearch = () => {
  primeSoftKeyboard()
  close()
  searchOpen.value = true
}
useKeyboardShortcut("note-search", openIntoSearch, () => !!props.user)

const { upperNavItems, lowerNavItems } = useNavigationItems()
const activeItem = computed(() =>
  [...upperNavItems.value, ...lowerNavItems.value].find((item) => item.isActive)
)
const { resumeRecall } = useResumeRecall()
const { goToNextAssimilation } = useGoToNextAssimilation()

const route = useRoute()
watch(() => route.fullPath, close)

const closeOnEscape = (event: KeyboardEvent) => {
  if (event.key === "Escape") close()
}
onMounted(() => document.addEventListener("keydown", closeOnEscape))
onUnmounted(() => document.removeEventListener("keydown", closeOnEscape))
</script>

<style scoped lang="scss">
@use "@/assets/page-bar.scss" as *;

.search-launcher:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: 2px;
}

.donut-menu > .daisy-dropdown-content {
  width: max-content;
  max-width: calc(100vw - 2rem);
  max-height: calc(100dvh - #{$page-bar-bottom});
  overflow-y: auto;
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
