<script setup lang="ts">
import DailyProbe from "@/components/recall/DailyProbe.vue"
import { useDailyProbeOffer } from "@/composables/useDailyProbeOffer"
import type { User } from "@generated/donut-backend-api"
import { inject, type Ref } from "vue"

const currentUser = inject<Ref<User | undefined>>("currentUser")
const {
  showDailyProbe,
  showOrdinaryRecall,
  showOfferRetry,
  checkOffer,
  markDailyProbeFinished,
} = useDailyProbeOffer(currentUser)
</script>

<template>
  <DailyProbe
    v-if="showDailyProbe"
    @complete="markDailyProbeFinished"
  />
  <div
    v-else-if="showOfferRetry"
    class="h-full flex flex-col items-center justify-center gap-6 p-6 text-center"
  >
    <button
      type="button"
      data-testid="daily-probe-offer-retry"
      class="daisy-btn"
      @click="checkOffer"
    >
      Retry
    </button>
  </div>
  <template v-else-if="showOrdinaryRecall">
    <slot />
  </template>
</template>
