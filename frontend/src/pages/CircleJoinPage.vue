<template>
  <ContainerPage
    v-bind="{ loading: false, contentExists: true, title: 'Join Circle' }"
  >
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script>
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import loginOrRegisterAndHaltThisThread from "../managedApi/window/loginOrRegisterAndHaltThisThread";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default {
  setup() {
    return useStoredLoadingApi();
  },
  components: { CircleJoinForm, ContainerPage },
  props: { invitationCode: Number },
  computed: {
    user() {
      return this.piniaStore.currentUser;
    },
  },
  beforeRouteEnter(to, from, next) {
    next((vm) => {
      if (!vm.user) {
        loginOrRegisterAndHaltThisThread();
        next(false);
      }
      next();
    });
  },
};
</script>
