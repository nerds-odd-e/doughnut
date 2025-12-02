<script setup lang="ts">
import type { Ref } from "vue"
import { computed, onMounted, provide, ref } from "vue"
import Popups from "./components/commons/Popups/Popups.vue"
import TestMenu from "./components/commons/TestMenu.vue"
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue"
import type { ApiStatus } from "./managedApi/ApiStatusHandler"
import { setupGlobalClient, nonReloadingClient } from "./managedApi/clientSetup"
import LoadingThinBar from "./components/commons/LoadingThinBar.vue"
import type { User } from "@generated/backend"
import {
  CurrentUserInfoController,
  TestabilityRestController,
} from "@generated/backend/sdk.gen"
import getEnvironment from "./managedApi/window/getEnvironment"
import MainMenu from "./components/toolbars/MainMenu.vue"

const apiStatus: Ref<ApiStatus> = ref({
  states: [],
})
// Initialize global client with interceptors for direct service imports
setupGlobalClient(apiStatus.value)
const user = ref<User | undefined>()
provide("currentUser", user)

const externalIdentifier = ref<string | undefined>()
const featureToggle = ref(false)
const environment = ref("production")
const userLoaded = ref(false)

const newUser = computed(() => {
  return !user.value && !!externalIdentifier.value
})

onMounted(async () => {
  environment.value = getEnvironment()
  if (environment.value === "testing") {
    const { data: toggle, error: toggleError } =
      await TestabilityRestController.getFeatureToggle({
        client: nonReloadingClient,
      })
    if (!toggleError) {
      featureToggle.value = toggle!
    }
  }
  const { data: userInfo, error: userError } =
    await CurrentUserInfoController.currentUserInfo({
      client: nonReloadingClient,
    })
  if (!userError) {
    user.value = userInfo!.user
    externalIdentifier.value = userInfo!.externalIdentifier
  }
  userLoaded.value = true
})
</script>

<template>
  <Popups />
  <LoadingThinBar v-if="user && apiStatus.states.length > 0" />
  <div class="daisy-flex daisy-bg-base-100 daisy-text-base-content app-container">
    <div class="main-menu daisy-flex daisy-bg-neutral daisy-text-neutral-content daisy-z-[10000]">
      <MainMenu
        :user="user"
        @update-user="user = $event"
      />
    </div>
    <div class="daisy-flex daisy-flex-col daisy-flex-grow daisy-overflow-y-auto daisy-overflow-x-hidden main-content">
      <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
      <template v-else-if="userLoaded">
        <router-view v-slot="{ Component }">
          <KeepAlive :include="['RecallPage']">
            <component :is="Component" />
          </KeepAlive>
        </router-view>
      </template>
    </div>
  </div>
  <TestMenu
    v-if="environment === 'testing'"
    :feature-toggle="featureToggle"
    :user="user"
    @feature-toggle="featureToggle = $event"
  />
</template>

<style scoped lang="scss">
@import "@/assets/menu-variables.scss";

.app-container {
  height: 100vh; // Default to viewport height for desktop (vertical menu)
  height: 100dvh; // Dynamic viewport height for mobile devices
  display: flex;
  overflow: hidden; // Prevent scrolling on desktop
}

.main-menu {
  height: 100%;
  width: $main-menu-width;
  position: fixed; // Sticky for vertical menu on desktop
  flex-shrink: 0;
}

.main-content {
  margin-left: $main-menu-width;
  height: 100%;
  min-width: 0;
  flex: 1;
  overflow-y: auto; // Only content area scrolls on desktop
}

@media (max-width: theme('screens.lg')) {
  .app-container {
    height: 100vh; // Keep fixed height for proper page height calculations
    height: 100dvh; // Dynamic viewport height for mobile devices
    flex-direction: column;
    overflow: hidden; // Prevent container scrolling
  }

  .main-menu {
    position: fixed; // Fixed at top, always visible
    top: 0;
    left: 0;
    width: auto;
    height: $main-menu-height-tablet;
    align-self: flex-start; // Align to left, not full width
    z-index: 200;
    background-color: transparent; // Remove parent background so menu-wrapper background shows
  }

  .main-content {
    margin-left: 0;
    width: 100%;
    min-width: 0;
    height: 100%; // Full height for proper page calculations
    overflow-y: auto; // Allow content area to scroll independently
  }
}

@media (max-width: theme('screens.md')) {
  .main-menu {
    height: $main-menu-height-mobile;
    background-color: transparent; // Remove parent background so menu-wrapper background shows
  }

  .main-content {
    width: 100%;
    min-width: 0;
    height: 100%; // Full height for proper page calculations
    overflow-y: auto; // Allow content area to scroll independently
  }
}
</style>
