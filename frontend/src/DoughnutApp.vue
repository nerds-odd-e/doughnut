<script lang="ts">
import { defineComponent, provide, Ref, ref } from "vue";
import Popups from "./components/commons/Popups/Popups.vue";
import TestMenu from "./components/commons/TestMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import { withLoadingApi } from "./managedApi/useLoadingApi";
import usePopups from "./components/commons/Popups/usePopups";
import createNoteStorage from "./store/createNoteStorage";
import ManagedApi, { ApiStatus } from "./managedApi/ManagedApi";
import GlobalBar from "./components/toolbars/GlobalBar.vue";
import { User } from "./generated/backend";
import getEnvironment from "./managedApi/window/getEnvironment";

export default defineComponent({
  setup() {
    const apiStatus: Ref<ApiStatus> = ref({
      errors: [],
      states: [],
    });
    const managedApi = new ManagedApi(apiStatus.value);
    const storageAccessor = createNoteStorage(managedApi);
    provide("managedApi", managedApi);

    return {
      apiStatus,
      storageAccessor: ref(storageAccessor),
      ...withLoadingApi(managedApi),
      ...usePopups(),
    };
  },
  data() {
    return {
      externalIdentifier: undefined as undefined | string,
      user: undefined as undefined | User,
      featureToggle: false,
      environment: "production",
      userLoaded: false,
    };
  },

  components: {
    Popups,
    TestMenu,
    UserNewRegisterPage,
    GlobalBar,
  },

  methods: {
    clearErrorMessage(_id: number) {
      this.apiStatus.errors = [];
    },
  },

  computed: {
    newUser() {
      return !this.user && !!this.externalIdentifier;
    },
    routeViewProps() {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const props = {} as any;
      if (this.$route.meta.useNoteStorageAccessor) {
        props.storageAccessor = this.storageAccessor;
      }
      if (this.$route.meta.userProp) {
        props.user = this.user;
      }
      return props;
    },
  },

  async mounted() {
    this.environment = getEnvironment();
    this.featureToggle =
      this.environment === "testing" &&
      (await this.managedApi.testabilityRestController.getFeatureToggle());
    const userInfo =
      await this.managedApi.restCurrentUserInfoController.currentUserInfo();
    this.user = userInfo.user;
    this.externalIdentifier = userInfo.externalIdentifier;
    this.userLoaded = true;
  },
});
</script>

<template>
  <Popups />
  <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
  <template v-else>
    <template v-if="userLoaded">
      <GlobalBar
        v-bind="{ storageAccessor, user, apiStatus }"
        @update-user="user = $event"
        @clear-error-message="clearErrorMessage($event)"
      />
      <router-view v-bind="routeViewProps" />
    </template>
    <TestMenu
      v-if="environment === 'testing'"
      :feature-toggle="featureToggle"
      :user="user"
      @feature-toggle="featureToggle = $event"
    />
  </template>
</template>
