<template>
  <ContainerPage v-bind="{ loading: false, contentExists: true }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { RouteLocationNormalized, NavigationGuardNext } from "vue-router";
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import loginOrRegisterAndHaltThisThread from "../managedApi/window/loginOrRegisterAndHaltThisThread";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  components: { CircleJoinForm, ContainerPage },
  props: {
    invitationCode: Number,
    user: {
      type: Object as PropType<Generated.User>,
      required: false,
    },
  },
  beforeRouteEnter(
    _to: RouteLocationNormalized,
    _from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) {
    next((vm) => {
      // eslint-disable-next-line dot-notation
      if (!vm["user"]) {
        loginOrRegisterAndHaltThisThread();
        next(false);
      }
      next();
    });
  },
});
</script>
