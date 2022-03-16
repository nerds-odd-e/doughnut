<script>
import Popups from "./components/commons/Popups.vue";
import MainMenu from "./components/commons/MainMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import useStoredLoadingApi from "./managedApi/useStoredLoadingApi";

export default {
  setup() {
    return useStoredLoadingApi({initalLoading: true, skipLoading: true})
  },
  data() {
    return {
      externalIdentifier: null,
      showNavBar: true,
    };
  },

  components: { Popups, MainMenu, UserNewRegisterPage },

  watch: {
    $route(to, from) {
      this.piniaStore.popupInfo = undefined
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
    popupInfo() { return this.piniaStore.popupInfo },
  },

  methods: {
    done(result) {
      if(this.piniaStore.popupInfo.doneResolve) this.piniaStore.popupInfo.doneResolve(result);
      this.piniaStore.popupInfo = undefined;
    },
  },

  mounted() {
    this.storedApi.getFeatureToggle()

    this.storedApi.getCurrentUserInfo().then((res) => {
      this.externalIdentifier = res.externalIdentifier;
    })
    .finally(()=> this.loading = false )

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
