<script lang="ts">
import { defineComponent, provide, Ref, ref } from "vue";
import Popups from "./components/commons/Popups/Popups.vue";
import TestMenu from "./components/commons/TestMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import { withLoadingApi } from "./managedApi/useLoadingApi";
import usePopups from "./components/commons/Popups/usePopups";
import ControlCenter from "./components/toolbars/ControlCenter.vue";
import createNoteStorage from "./store/createNoteStorage";
import BreadcrumbMain from "./components/toolbars/BreadcrumbMain.vue";
import ManagedApi, { ApiStatus } from "./managedApi/ManagedApi";

export default defineComponent({
  setup() {
    const apiStatus: Ref<ApiStatus> = ref({
      errors: [],
      states: [],
    });
    ManagedApi.registerStatus(apiStatus.value);
    const managedApi = new ManagedApi();
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
      user: undefined as undefined | Generated.User,
      featureToggle: false,
      environment: "production",
      userLoaded: false,
    };
  },

  components: {
    Popups,
    TestMenu,
    UserNewRegisterPage,
    ControlCenter,
    BreadcrumbMain,
  },

  watch: {
    $route() {
      this.popups.done(false);
    },
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
    this.environment = this.api.testability.getEnvironment();
    this.featureToggle = await this.api.testability.getFeatureToggle();
    const userInfo = await this.api.userMethods.getCurrentUserInfo();
    this.user = userInfo.user;
    this.externalIdentifier = userInfo.externalIdentifier;
    this.userLoaded = true;
  },
});
</script>

<template>
  <div class="box">
    <Popups />
    <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
    <template v-else>
      <template v-if="userLoaded">
        <div class="header">
          <BreadcrumbMain v-bind="{ storageAccessor, user }" />
          <ControlCenter
            v-bind="{ storageAccessor, user, apiStatus }"
            @update-user="user = $event"
            @clear-error-message="clearErrorMessage($event)"
          />
        </div>
        <router-view v-bind="routeViewProps" />
      </template>
      <TestMenu
        v-if="environment === 'testing'"
        :feature-toggle="featureToggle"
        :user="user"
        @feature-toggle="featureToggle = $event"
      />
    </template>
  </div>
</template>

<style lang="sass" scoped>
.box
  display: flex
  flex-direction: column
  height: 100vh
  max-height: -webkit-fill-available

.box .header
  flex: 0 0 auto

.box .content
  flex: 1
  -ms-flex: 1 1 auto
  overflow-y: auto

.box .footer
  flex: 0 1 40px
</style>

<style lang="sass">
.inner-box
  display: flex
  flex-direction: column
  height: 100%

.inner-box .header
  flex: 0 1 auto

.inner-box .content
  flex: 1
  overflow-y: auto

.inner-box .footer
  flex: 0 1 40px
</style>
