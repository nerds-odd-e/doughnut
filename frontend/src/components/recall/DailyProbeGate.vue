<script setup lang="ts">
import DailyProbe from "@/components/recall/DailyProbe.vue"
import { useDailyProbeOffer } from "@/composables/useDailyProbeOffer"
import type { User } from "@generated/donut-backend-api"
import { inject, type Ref } from "vue"

const currentUser = inject<Ref<User | undefined>>("currentUser")
const { showDailyProbe, showOrdinaryRecall, markDailyProbeFinished } =
  useDailyProbeOffer(currentUser)
</script>

<template>
  <DailyProbe
    v-if="showDailyProbe"
    @complete="markDailyProbeFinished"
  />
  <template v-else-if="showOrdinaryRecall">
    <slot />
  </template>
</template>
