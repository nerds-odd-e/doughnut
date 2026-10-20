<script setup lang="ts">
import type { Ref } from "vue"
import { computed, onMounted, provide, ref } from "vue"
import Popups from "./components/commons/Popups/Popups.vue"
import TestMenu from "./components/commons/TestMenu.vue"
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue"
import {
  currentBlockingApiState,
  type ApiStatus,
} from "./managedApi/ApiStatusHandler"
import { setupGlobalClient, nonReloadingClient } from "./managedApi/clientSetup"
import LoadingThinBar from "./components/commons/LoadingThinBar.vue"
import LoadingModal from "./components/commons/LoadingModal.vue"
import type { User } from "@generated/donut-backend-api"
import {
  CurrentUserInfoController,
  TestabilityRestController,
} from "@generated/donut-backend-api/sdk.gen"
import getEnvironment from "./managedApi/window/getEnvironment"
import SoftKeyboardPrimer from "./components/commons/SoftKeyboardPrimer.vue"
import FrontendUpdateReminder from "./components/commons/FrontendUpdateReminder.vue"
import { useFeatureToggle } from "./composables/useFeatureToggle"
import { useSignedInMenuData } from "./composables/useSignedInMenuData"
import { migrateSearchKeyHistory } from "./utils/searchKeyHistory"

migrateSearchKeyHistory()

const apiStatus: Ref<ApiStatus> = ref({
  states: [],
})
// Initialize global client with interceptors for direct service imports
setupGlobalClient(apiStatus.value)
const user = ref<User | undefined>()
provide("currentUser", user)
useSignedInMenuData(user)
const blockingApiState = computed(() =>
  currentBlockingApiState(apiStatus.value)
)

const externalIdentifier = ref<string | undefined>()
const { featureToggle, setFeatureToggle } = useFeatureToggle()
const environment = ref("production")
const userLoaded = ref(false)

const newUser = computed(() => !user.value && !!externalIdentifier.value)

onMounted(async () => {
  environment.value = getEnvironment()
  if (environment.value === "testing") {
    const { data: toggle, error: toggleError } =
      await TestabilityRestController.getFeatureToggle({
        client: nonReloadingClient,
      })
    if (!toggleError) {
      setFeatureToggle(toggle!)
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
  <SoftKeyboardPrimer />
  <Popups />
  <FrontendUpdateReminder />
  <LoadingThinBar v-if="apiStatus.states.length > 0" />
  <LoadingModal
    :show="!!blockingApiState"
    :message="blockingApiState?.message"
    :cancel-control="
      blockingApiState?.cancel
        ? { id: blockingApiState.id, action: blockingApiState.cancel }
        : undefined
    "
  />
  <div class="flex flex-col overflow-y-auto overflow-x-hidden bg-base-100 text-base-content app-container">
    <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
    <template v-else-if="userLoaded">
      <router-view v-slot="{ Component }">
        <KeepAlive :include="['RecallPage']">
          <component :is="Component" />
        </KeepAlive>
      </router-view>
    </template>
  </div>
  <TestMenu
    v-if="environment === 'testing'"
    :feature-toggle="featureToggle"
    :user="user"
    @feature-toggle="setFeatureToggle($event)"
  />
</template>

<style scoped lang="scss">
.app-container {
  height: 100vh;
  height: 100dvh; // Dynamic viewport height for mobile devices
}
</style>
