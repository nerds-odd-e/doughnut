<script setup lang="ts">
import type { Ref } from "vue"
import { computed, onMounted, provide, ref } from "vue"
import { useRoute } from "vue-router"
import Popups from "./components/commons/Popups/Popups.vue"
import TestMenu from "./components/commons/TestMenu.vue"
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue"
import createNoteStorage from "./store/createNoteStorage"
import type { ApiStatus } from "./managedApi/ManagedApi"
import ManagedApi from "./managedApi/ManagedApi"
import { setupGlobalClient } from "./managedApi/clientSetup"
import GlobalBar from "./components/toolbars/GlobalBar.vue"
import type { User } from "@generated/backend"
import getEnvironment from "./managedApi/window/getEnvironment"
import MainMenu from "./components/toolbars/MainMenu.vue"

interface RouteViewProps {
  storageAccessor?: typeof storageAccessor.value
  [key: string]: unknown
}

const apiStatus: Ref<ApiStatus> = ref({
  states: [],
  errors: [],
})
// Initialize global client with interceptors for direct service imports
setupGlobalClient(apiStatus.value)
const managedApi = new ManagedApi(apiStatus.value)
provide("managedApi", managedApi)
const user = ref<User | undefined>()
provide("currentUser", user)

const storageAccessor = ref(createNoteStorage(managedApi))
const $route = useRoute()
const externalIdentifier = ref<string | undefined>()
const featureToggle = ref(false)
const environment = ref("production")
const userLoaded = ref(false)

const newUser = computed(() => {
  return !user.value && !!externalIdentifier.value
})

const routeViewProps = computed(() => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const props: RouteViewProps = {}
  if ($route.meta.useNoteStorageAccessor) {
    props.storageAccessor = storageAccessor.value
  }
  return props
})

onMounted(async () => {
  environment.value = getEnvironment()
  featureToggle.value =
    environment.value === "testing" &&
    (await managedApi.services.getFeatureToggle())
  const userInfo = await managedApi.services.currentUserInfo()
  user.value = userInfo.user
  externalIdentifier.value = userInfo.externalIdentifier
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
      <div class="daisy-sticky daisy-top-0 daisy-z-100 global-bar">
        <GlobalBar
          v-bind="{ storageAccessor, user, apiStatus }"
          @update-user="user = $event"
        />
      </div>
      <div class="daisy-flex-grow daisy-overflow-y-auto main-content">
        <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
        <template v-else-if="userLoaded">
          <router-view v-slot="{ Component }">
            <KeepAlive :include="['RecallPage']">
              <component v-bind="routeViewProps" :is="Component" />
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
$global-bar-height: 51px;

.main-menu {
  height: 100%;
  width: $main-menu-width;
  position: fixed;
}

.path-and-content {
  margin-left: $main-menu-width;
  height: 100%;
}

.global-bar {
  height: $global-bar-height;
  max-width: 100vw;
}

@media (min-width: theme('screens.lg')) {
  .global-bar {
    max-width: calc(100vw - #{$main-menu-width});
  }
}

.main-content {
  height: calc(100% - #{$global-bar-height});
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
  }

  .main-content {
    height: calc(100% - #{$global-bar-height});
  }
}

@media (max-width: theme('screens.md')) {
  .main-menu {
    height: $main-menu-height-mobile;
  }

  .path-and-content {
    margin-top: $main-menu-height-mobile;
    height: calc(100% - #{$main-menu-height-mobile});
  }

  .main-content {
    height: calc(100% - #{$global-bar-height});
  }
}
</style>
