<script setup lang="ts">
import type { Ref } from "vue"
import { computed, onMounted, provide, ref } from "vue"
import Popups from "./components/commons/Popups/Popups.vue"
import TestMenu from "./components/commons/TestMenu.vue"
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue"
import type { ApiStatus } from "./managedApi/ApiStatusHandler"
import { setupGlobalClient, nonReloadingClient } from "./managedApi/clientSetup"
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
provide("apiStatus", apiStatus)

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
  <div class="daisy-flex daisy-h-dvh daisy-bg-base-100 daisy-text-base-content">
    <div class="main-menu daisy-flex daisy-bg-neutral daisy-text-neutral-content daisy-z-[10000]">
      <MainMenu
        :user="user"
        @update-user="user = $event"
      />
    </div>
    <div class="daisy-flex daisy-flex-col daisy-flex-grow path-and-content">
      <div class="daisy-flex-grow daisy-overflow-y-auto daisy-overflow-x-hidden main-content">
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
  </div>
  <TestMenu
    v-if="environment === 'testing'"
    :feature-toggle="featureToggle"
    :user="user"
    @feature-toggle="featureToggle = $event"
  />
</template>

<style scoped lang="scss">
$main-menu-width: 90px;
$main-menu-height-tablet: 70px;
$main-menu-height-mobile: 55px;

.main-menu {
  height: 100%;
  width: $main-menu-width;
  position: fixed;
}

.path-and-content {
  margin-left: $main-menu-width;
  height: 100%;
  min-width: 0;
}

.main-content {
  height: 100%;
}

@media (max-width: theme('screens.lg')) {
  .main-menu {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: $main-menu-height-tablet;
    z-index: 200;
  }

  .path-and-content {
    margin-left: 0;
    margin-top: $main-menu-height-tablet;
    height: calc(100% - #{$main-menu-height-tablet});
    min-width: 0;
  }

  .main-content {
    height: 100%;
  }
}

@media (max-width: theme('screens.md')) {
  .main-menu {
    height: $main-menu-height-mobile;
  }

  .path-and-content {
    margin-top: $main-menu-height-mobile;
    height: calc(100% - #{$main-menu-height-mobile});
    min-width: 0;
  }

  .main-content {
    height: 100%;
  }
}
</style>
