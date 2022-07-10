<script>
import Popups from "./components/commons/Popups/Popups.vue";
import MainMenu from "./components/commons/MainMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import useStoredLoadingApi from "./managedApi/useStoredLoadingApi";
import usePopups from "./components/commons/Popups/usePopup";
import ReviewDoughnut from "./components/review/ReviewDoughnut.vue";
import LoginButton from "./components/toolbars/LoginButton.vue";

export default {
  setup() {
    return {
      ...useStoredLoadingApi({ initalLoading: true, skipLoading: true }),
      ...usePopups(),
    };
  },
  data() {
    return {
      reviewData: null,
      externalIdentifier: null,
      showNavBar: true,
    };
  },

  components: {
    Popups,
    MainMenu,
    UserNewRegisterPage,
    ReviewDoughnut,
    LoginButton,
  },

  watch: {
    $route(to) {
      this.popups.done(false);
      if (to.name) {
        this.showNavBar = !["repeat", "initial"].includes(
          to.name.split("-").shift()
        );
      }
    },
  },

  computed: {
    newUser() {
      return !this.user && !!this.externalIdentifier;
    },
    user() {
      return this.piniaStore.currentUser;
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
};
</script>

<template>
  <div class="box">
    <Popups />
    <UserNewRegisterPage v-if="newUser" />
    <template v-else>
      <div class="header">
        <MainMenu v-if="showNavBar" />
      </div>
      <div v-if="!loading" class="content">
        <router-view @update-reviewing="reviewData = $event" />
      </div>
      <ReviewDoughnut v-if="user" :user="user" :review-data="reviewData" />
      <LoginButton v-else />
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
