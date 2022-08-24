<template>
  <ContainerPage v-bind="{ loading: false, contentExists: true }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script lang="js">
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import loginOrRegisterAndHaltThisThread from "../managedApi/window/loginOrRegisterAndHaltThisThread";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default {
  setup() {
    return useStoredLoadingApi();
  },
  components: { CircleJoinForm, ContainerPage },
  props: {
    invitationCode: Number,
    user: {
      type: Object,
      required: false,
    },
  },
  beforeRouteEnter(
    _to,
    _from,
    next
  ) {
    next(async (vm) => {
      const x = await vm.storedApi.getCurrentUserInfo();
      if (!x?.user) {
        loginOrRegisterAndHaltThisThread();
        next(false);
      }
      next();
    });
  },
};
</script>
