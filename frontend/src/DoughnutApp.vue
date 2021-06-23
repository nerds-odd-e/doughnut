<script setup>
import { ref } from "@vue/reactivity"
import {restGet} from "./restful/restful"

const staticInfo = ref(null)
const loading = ref(null)
const redirectTo = (params) => {
        this.$router.push(params)
      }

const fetchStaticInfo = async () => {
  restGet(`/api/static-info`, loading, (res) => staticInfo.value = res)
}

fetchStaticInfo()

</script>

<template>
  <router-view @redirect="redirectTo($event)" v-bind="{staticInfo}"></router-view>
</template>
