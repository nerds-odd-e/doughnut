<template>
  <ContainerPage v-bind="{ title: 'Joining a circle' }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue"
import type { RouteLocationNormalized, NavigationGuardNext } from "vue-router"
import CircleJoinForm from "@/components/circles/CircleJoinForm.vue"
import { CurrentUserInfoController } from "@generated/backend/sdk.gen"
import { globalClientSilent } from "@/managedApi/clientSetup"
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import ContainerPage from "./commons/ContainerPage.vue"

export default defineComponent({
  components: { CircleJoinForm, ContainerPage },
  props: {
    invitationCode: Number,
  },
  beforeRouteEnter(
    _to: RouteLocationNormalized,
    _from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) {
    next(async () => {
      const { data: userInfo, error } =
        await CurrentUserInfoController.currentUserInfo({
          client: globalClientSilent,
        })
      if (error || !userInfo?.user) {
        loginOrRegisterAndHaltThisThread()
        next(false)
      } else {
        next()
      }
    })
  },
})
</script>
