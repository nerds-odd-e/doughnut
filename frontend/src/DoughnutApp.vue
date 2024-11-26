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
  <div class="app-container">
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
          <router-view v-bind="routeViewProps" />
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

.app-container {
  display: flex;
  min-height: 100vh;
}

.sidebar-control {
  display: flex;
  background-color: #2d2d2d;
  flex-direction: column;
  height: 100vh;
  width: 64px;
  color: #e0e0e0;
  position: fixed;
  z-index: 10000;
}

.path-and-content {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  margin-left: 64px;
}

.sticky-top {
  position: sticky;
  top: 0;
  z-index: 100;
}

.main-content {
  flex-grow: 1;
  overflow-y: auto;
}

@media (max-width: $tablet-breakpoint) {
  .app-container {
    flex-direction: column;
  }

  .sidebar-control {
    position: fixed;
    top: 0;
    left: 0;
    flex-direction: row;
    width: 100%;
    height: 70px;
    padding: 0.5rem;
    z-index: 200;
  }

  .path-and-content {
    margin-left: 0;
    margin-top: 70px;
  }
}

@media (max-width: $mobile-breakpoint) {
  .sidebar-control {
    height: 55px;
  }

  .path-and-content {
    margin-top: 55px;
  }
}
</style>
