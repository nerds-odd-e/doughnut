<script>
import Popups from "./components/commons/Popups.vue";
import MainMenu from "./components/commons/MainMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import useStoredLoadingApi from "./managedApi/useStoredLoadingApi";

export default {
  setup() {
    return useStoredLoadingApi({initalLoading: true})
  },
  data() {
    return {
      externalIdentifier: null,
      showNavBar: true,
      popupInfo: null,
      doneResolve: null,
    };
  },

  components: { Popups, MainMenu, UserNewRegisterPage },

  watch: {
    $route(to, from) {
      this.popupInfo = null
      if (to.name) {
        this.showNavBar = !["repeat", "initial"].includes(
          to.name.split("-").shift()
        );
      }
    },
  },

  computed: {
    newUser() { return !this.user && !!this.externalIdentifier; },
    user() { return this.piniaStore.currentUser },
  },

  methods: {
    done(result) {
      this.doneResolve(result);
      this.popupInfo = null;
      this.doneResolve = null;
    },
  },

  mounted() {
    this.storedApi({skipLoading: true}).getFeatureToggle()

    this.storedApi().getCurrentUserInfo().then((res) => {
      this.externalIdentifier = res.externalIdentifier;
    })

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
  <UserNewRegisterPage v-if="newUser"/>
  <template v-else>
    <div class="header">
      <MainMenu v-if="showNavBar" />
    </div>
    <div v-if="!loading" class="content">
      <router-view/>
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
