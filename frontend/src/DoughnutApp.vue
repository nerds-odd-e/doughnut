<script>
import Popups from "./components/commons/Popups.vue";
import MainMenu from "./components/commons/MainMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import { apiGetCurrentUserInfo, apiGetFeatureToggle } from "./storedApi"

export default {
  data() {
    return {
      featureToggle: false,
      user: null,
      externalIdentifier: null,
      showNavBar: true,
      loading: true,
      popupInfo: [],
      doneResolve: null,
    };
  },

  components: { Popups, MainMenu, UserNewRegisterPage },

  watch: {
    $route(to, from) {
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
  },

  methods: {
    done(result) {
      this.doneResolve(result);
      this.popupInfo = null;
      this.doneResolve = null;
    },
  },

  mounted() {
    this.loading = true

    apiGetFeatureToggle().then((res) => { this.featureToggle = res})

    apiGetCurrentUserInfo().then((res) => {
      this.user = res.user;
      this.externalIdentifier = res.externalIdentifier;
    })
    .finally(() => this.loading = false)

    this.$popups.alert = (msg) => {
      this.popupInfo = { type: "alert", message: msg };
      return new Promise((resolve, reject) => {
        this.doneResolve = resolve;
      });
    };

    this.$popups.confirm = (msg) => {
      this.popupInfo = { type: "confirm", message: msg };
      return new Promise((resolve, reject) => {
        this.doneResolve = resolve;
      });
    };

    this.$popups.dialog = (component, attrs) => {
      this.popupInfo = { type: "dialog", component, attrs };
      return new Promise((resolve, reject) => {
        this.doneResolve = resolve;
      });
    };
  },
};
</script>

<template>
<div class="box">

  <Popups :popupInfo="popupInfo" @done="done($event)" />
  <UserNewRegisterPage v-if="newUser" @userCreated="user = $event" />
  <template v-else>
    <div class="header">
      <MainMenu v-if="showNavBar" v-bind="{user, featureToggle}" />
    </div>
    <div v-if="!loading" class="content">
      <router-view :user="user" @userUpdated="user = $event" :featureToggle="featureToggle" />
    </div>
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