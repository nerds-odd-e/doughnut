<template>
  <ContainerPage v-bind="{ loading: false, contentExists: true, title: 'Join Circle' }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script>
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import loginOrRegister from '../managedApi/restful/loginOrRegister';
import { useStore } from "@/store";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
  components: { CircleJoinForm, ContainerPage },
  props: { invitationCode: Number },
  computed: {
    user() { return this.store.getCurrentUser()},
  },
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
