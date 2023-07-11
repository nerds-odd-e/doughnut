<template>
  <ContainerPage v-bind="{ contentExists: true }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script lang="js">
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import loginOrRegisterAndHaltThisThread from "../managedApi/window/loginOrRegisterAndHaltThisThread";
import useLoadingApi from "../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  components: { CircleJoinForm, ContainerPage },
  props: {
    invitationCode: Number,
    user: {
      type: Object,
      required: false,
    },
  },
  beforeRouteEnter(_to, _from, next) {
    next(async (vm) => {
      const x = await vm.api.userMethods.getCurrentUserInfo();
      if (!x?.user) {
        loginOrRegisterAndHaltThisThread();
        next(false);
      }
      next();
    });
  },
};
</script>
