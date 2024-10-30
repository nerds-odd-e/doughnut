<template>
  <ContainerPage v-bind="{ contentExists: true, title: 'Joining a circle' }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script lang="js">
import CircleJoinForm from "@/components/circles/CircleJoinForm.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import ContainerPage from "./commons/ContainerPage.vue"

export default {
  setup() {
    return useLoadingApi()
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
      const x =
        await vm.managedApi.restCurrentUserInfoController.currentUserInfo()
      if (!x?.user) {
        loginOrRegisterAndHaltThisThread()
        next(false)
      }
      next()
    })
  },
}
</script>
