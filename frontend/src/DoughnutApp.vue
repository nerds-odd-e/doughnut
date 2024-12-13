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
import GlobalBar from "./components/toolbars/GlobalBar.vue"
import type { User } from "./generated/backend"
import getEnvironment from "./managedApi/window/getEnvironment"
import SidebarControl from "./components/toolbars/SidebarControl.vue"

interface RouteViewProps {
  storageAccessor?: typeof storageAccessor.value
  [key: string]: unknown
}

const apiStatus: Ref<ApiStatus> = ref({
  errors: [],
  states: [],
})
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

const clearErrorMessage = (_id: number) => {
  apiStatus.value.errors = []
}

onMounted(async () => {
  environment.value = getEnvironment()
  featureToggle.value =
    environment.value === "testing" &&
    (await managedApi.testabilityRestController.getFeatureToggle())
  const userInfo =
    await managedApi.restCurrentUserInfoController.currentUserInfo()
  user.value = userInfo.user
  externalIdentifier.value = userInfo.externalIdentifier
  userLoaded.value = true
})
</script>

<template>
  <Popups />
  <div class="daisy-flex daisy-h-dvh daisy-bg-base-100 daisy-text-base-content">
    <div class="sidebar-control">
      <SidebarControl
        :user="user"
        @update-user="user = $event"
      />
    </div>
    <div class="path-and-content">
      <div class="sticky-top">
        <GlobalBar
          v-bind="{ storageAccessor, user, apiStatus }"
          @update-user="user = $event"
          @clear-error-message="clearErrorMessage($event)"
        />
      </div>
      <div class="main-content">
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
@import '@/styles/_variables.scss';

$sidebar-width: 64px;
$sidebar-height-tablet: 70px;
$sidebar-height-mobile: 55px;
$global-bar-height: 51px;

.sidebar-control {
  display: flex;
  background-color: #2d2d2d;
  flex-direction: column;
  height: 100vh;
  width: $sidebar-width;
  color: #e0e0e0;
  position: fixed;
  z-index: 10000;
}

.path-and-content {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  margin-left: $sidebar-width;
  height: 100vh;
}

.sticky-top {
  position: sticky;
  top: 0;
  z-index: 100;
  height: $global-bar-height;
}

.main-content {
  flex-grow: 1;
  overflow-y: auto;
  height: calc(100vh - #{$global-bar-height});
}

@media (max-width: $tablet-breakpoint) {
  .path-and-content {
    margin-left: 0;
    margin-top: $sidebar-height-tablet;
    height: calc(100vh - #{$sidebar-height-tablet});
  }

  .main-content {
    height: calc(100vh - #{$sidebar-height-tablet} - #{$global-bar-height});
  }
}

@media (max-width: $mobile-breakpoint) {
  .path-and-content {
    margin-top: $sidebar-height-mobile;
    height: calc(100vh - #{$sidebar-height-mobile});
  }

  .main-content {
    height: calc(100vh - #{$sidebar-height-mobile} - #{$global-bar-height});
  }
}
</style>
