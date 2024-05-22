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

const sidebarCollapsedForSmallScreen = ref(true);
const toggleSideBar = () => {
  sidebarCollapsedForSmallScreen.value = !sidebarCollapsedForSmallScreen.value;
};

const newUser = computed(() => {
  return !user.value && !!externalIdentifier.value;
});

const routeViewProps = computed(() => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const props = {} as any;
  props.sidebarCollapsedForSmallScreen = sidebarCollapsedForSmallScreen.value;
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
  <div clas="d-flex flex-column vh-100">
    <GlobalBar
      v-bind="{ storageAccessor, user, apiStatus }"
      @update-user="user = $event"
      @clear-error-message="clearErrorMessage($event)"
      @toggle-sidebar="toggleSideBar"
    />
    <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
    <template v-else-if="userLoaded">
      <div class="overflow-auto h-full">
        <router-view v-bind="routeViewProps" />
      </div>
    </template>
  </div>
  <TestMenu
    v-if="environment === 'testing'"
    :feature-toggle="featureToggle"
    :user="user"
    @feature-toggle="featureToggle = $event"
  />
</template>

<style scoped lang="scss">
.h-full {
  height: calc(100vh - 4rem);
}
</style>
