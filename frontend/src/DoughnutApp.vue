<script lang="ts">
import { defineComponent } from "vue";
import Popups from "./components/commons/Popups/Popups.vue";
import TestMenu from "./components/commons/TestMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import useStoredLoadingApi from "./managedApi/useStoredLoadingApi";
import usePopups from "./components/commons/Popups/usePopup";
import ReviewDoughnut from "./components/review/ReviewDoughnut.vue";
import LoginButton from "./components/toolbars/LoginButton.vue";

export default defineComponent({
  setup() {
    return {
      ...useStoredLoadingApi({ initalLoading: true, skipLoading: true }),
      ...usePopups(),
    };
  },
  data() {
    return {
      externalIdentifier: undefined as undefined | string,
      showNavBar: true,
    };
  },

  components: {
    Popups,
    TestMenu,
    UserNewRegisterPage,
    ReviewDoughnut,
    LoginButton,
  },

  watch: {
    $route() {
      this.popups.done(false);
    },
  },

  computed: {
    newUser() {
      return !this.user && !!this.externalIdentifier;
    },
    user() {
      return this.piniaStore.currentUser;
    },
    environment() {
      return this.piniaStore.environment;
    },
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
  },

  mounted() {
    this.storedApi.testability.getFeatureToggle();

    this.storedApi
      .getCurrentUserInfo()
      .then((res) => {
        this.externalIdentifier = res.externalIdentifier;
      })
      .finally(() => (this.loading = false));
  },
});
</script>

<template>
  <div class="box">
    <Popups />
    <UserNewRegisterPage v-if="newUser" />
    <template v-else>
      <div v-if="!loading" class="content">
        <router-view />
      </div>
      <ReviewDoughnut v-if="user" :user="user" />
      <LoginButton v-else />
      <TestMenu
        v-if="environment === 'testing'"
        :feature-toggle="featureToggle"
        :user="user"
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
