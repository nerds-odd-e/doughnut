<script lang="ts">
import { defineComponent } from "vue";
import Popups from "./components/commons/Popups/Popups.vue";
import TestMenu from "./components/commons/TestMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import useLoadingApi from "./managedApi/useLoadingApi";
import usePopups from "./components/commons/Popups/usePopup";
import ControlCenter from "./components/toolbars/ControlCenter.vue";
import createNoteStorage from "./store/createNoteStorage";
import BreadcrumbMain from "./components/toolbars/BreadcrumbMain.vue";
import ManagedApi, { ApiStatus } from "./managedApi/ManagedApi";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
      ...usePopups(),
    };
  },
  data() {
    return {
      externalIdentifier: undefined as undefined | string,
      user: undefined as undefined | Generated.User,
      featureToggle: false,
      environment: "production",
      storageAccessor: createNoteStorage(),
      apiStatus: { states: [] } as ApiStatus,
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
    "storageAccessor.updatedAt": function updatedAt() {
      if (!this.storageAccessor.updatedNoteRealm) {
        this.$router.replace({ name: "notebooks" });
      }
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
    ManagedApi.registerStatus(this.apiStatus);
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
  flex-flow: column
  height: 100vh

.box .header
  flex: 0 1 auto

.box .content
  flex: 1 1 auto

.box .footer
  flex: 0 1 40px
</style>

<style lang="sass">
.inner-box
  display: flex
  flex-flow: column
  height: 100%

.inner-box .header
  flex: 0 1 auto

.inner-box .content
  flex: 1 1 auto
  overflow: hidden

.inner-box .footer
  flex: 0 1 40px
</style>
