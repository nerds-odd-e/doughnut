<template>
  <ContainerPage v-bind="{ title: 'Joining a circle' }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue"
import type { RouteLocationNormalized, NavigationGuardNext } from "vue-router"
import CircleJoinForm from "@/components/circles/CircleJoinForm.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import ContainerPage from "./commons/ContainerPage.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  components: { CircleJoinForm, ContainerPage },
  props: {
    invitationCode: Number,
  },
  beforeRouteEnter(
    _to: RouteLocationNormalized,
    _from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) {
    next(async (vm) => {
      const component = vm as unknown as {
        managedApi: ReturnType<typeof useLoadingApi>["managedApi"]
      }
      const x =
        await component.managedApi.restCurrentUserInfoController.currentUserInfo()
      if (!x?.user) {
        loginOrRegisterAndHaltThisThread()
        next(false)
      }
      next()
    })
  },
})
</script>
