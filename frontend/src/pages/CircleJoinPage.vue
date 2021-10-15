<template>
  <ContainerPage v-bind="{ loading, contentExists: !!formData, title: 'Join Circle' }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script>
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import { loginOrRegister } from "../restful/restful";

export default {
  components: { CircleJoinForm, ContainerPage },
  props: { invitationCode: Number, user: Object },

  beforeRouteEnter(to, from, next) {
    next((vm) => {
      if (!vm.user) {
        loginOrRegister();
        next(false);
      }
      next();
    });
  },
};
</script>
