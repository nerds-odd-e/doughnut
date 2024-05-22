<script setup lang="ts">
import { computed, onMounted, provide, Ref, ref } from "vue";
import { useRoute } from "vue-router";
import Popups from "./components/commons/Popups/Popups.vue";
import TestMenu from "./components/commons/TestMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import createNoteStorage from "./store/createNoteStorage";
import ManagedApi, { ApiStatus } from "./managedApi/ManagedApi";
import GlobalBar from "./components/toolbars/GlobalBar.vue";
import { User } from "./generated/backend";
import getEnvironment from "./managedApi/window/getEnvironment";

const apiStatus: Ref<ApiStatus> = ref({
  errors: [],
  states: [],
});
const managedApi = new ManagedApi(apiStatus.value);
const storageAccessor = ref(createNoteStorage(managedApi));
provide("managedApi", managedApi);
const $route = useRoute();

const externalIdentifier = ref<string | undefined>();
const user = ref<User | undefined>();
const featureToggle = ref(false);
const environment = ref("production");
const userLoaded = ref(false);

const newUser = computed(() => {
  return !user.value && !!externalIdentifier.value;
});
const routeViewProps = computed(() => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const props = {} as any;
  if ($route.meta.useNoteStorageAccessor) {
    props.storageAccessor = storageAccessor.value;
  }
  if ($route.meta.userProp) {
    props.user = user.value;
  }
  return props;
});

const clearErrorMessage = (_id: number) => {
  apiStatus.value.errors = [];
};

const sidebarCollapsedForSmallScreen = ref(true);
const toggleSideBar = () => {
  sidebarCollapsedForSmallScreen.value = !sidebarCollapsedForSmallScreen.value;
};

onMounted(async () => {
  environment.value = getEnvironment();
  featureToggle.value =
    environment.value === "testing" &&
    (await managedApi.testabilityRestController.getFeatureToggle());
  const userInfo =
    await managedApi.restCurrentUserInfoController.currentUserInfo();
  user.value = userInfo.user;
  externalIdentifier.value = userInfo.externalIdentifier;
  userLoaded.value = true;
});
</script>

<template>
  <Popups />
  <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
  <template v-else>
    <template v-if="userLoaded">
      <div clas="d-flex flex-column vh-100">
        <GlobalBar
          v-bind="{ storageAccessor, user, apiStatus }"
          @update-user="user = $event"
          @clear-error-message="clearErrorMessage($event)"
          @toggle-sidebar="toggleSideBar"
        />
        <div class="overflow-auto h-full">
          <div class="d-flex flex-grow-1">
            <aside
              class="d-lg-block flex-shrink-0 overflow-auto"
              :class="{ 'd-none': sidebarCollapsedForSmallScreen }"
            >
              <NoteSidebar
                v-bind="{
                  storageAccessor,
                }"
              />
            </aside>
            <main
              class="flex-grow-1 overflow-auto"
              :class="{ 'd-none': !sidebarCollapsedForSmallScreen }"
            >
              <router-view v-bind="routeViewProps" />
            </main>
          </div>
        </div>
      </div>
    </template>
    <TestMenu
      v-if="environment === 'testing'"
      :feature-toggle="featureToggle"
      :user="user"
      @feature-toggle="featureToggle = $event"
    />
  </template>
</template>

<style scoped lang="scss">
@import "bootstrap/scss/bootstrap";

aside {
  width: 100%;
  @include media-breakpoint-up(lg) {
    width: 18rem;
  }
}

.h-full {
  height: calc(100vh - 4rem);
}
</style>
