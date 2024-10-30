<template>
  <ContainerPage v-bind="{ contentExists: true, title: 'Joining a circle' }">
    <CircleJoinForm v-bind="{ invitationCode }" />
  </ContainerPage>
</template>

<script setup>
import CircleJoinForm from "@/components/circles/CircleJoinForm.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import ContainerPage from "./commons/ContainerPage.vue"
import { onBeforeRouteEnter } from "vue-router"

const props = defineProps({
  invitationCode: Number,
  user: {
    type: Object,
    required: false,
  },
})

const managedApi = useLoadingApi()

onBeforeRouteEnter(async (to, from, next) => {
  const x = await managedApi.restCurrentUserInfoController.currentUserInfo()
  if (!x?.user) {
    loginOrRegisterAndHaltThisThread()
    next(false)
    return
  }
  next()
})
</script>
