<script setup lang="ts">
import DailyProbe from "@/components/recall/DailyProbe.vue"
import { useDailyProbeOffer } from "@/composables/useDailyProbeOffer"
import { DAILY_PROBE_INSTRUCTION } from "@/models/dailyProbe"
import type { User } from "@generated/donut-backend-api"
import { inject, type Ref } from "vue"

const currentUser = inject<Ref<User | undefined>>("currentUser")
const {
  showDailyProbeEntry,
  showDailyProbe,
  showOrdinaryRecall,
  showOfferRetry,
  checkOffer,
  startDailyProbe,
  skipDailyProbe,
  markDailyProbeFinished,
} = useDailyProbeOffer(currentUser)
</script>

<template>
  <slot name="bar" :ordinary-recall="showOrdinaryRecall" />
  <div
    v-if="showDailyProbeEntry"
    data-testid="daily-probe-entry"
    class="h-full flex flex-col items-center justify-center gap-6 p-6 text-center"
  >
    <p data-testid="daily-probe-instruction">{{ DAILY_PROBE_INSTRUCTION }}</p>
    <div class="flex gap-4">
      <button
        type="button"
        data-testid="daily-probe-start"
        class="daisy-btn daisy-btn-primary"
        @click="startDailyProbe"
      >
        Start
      </button>
      <button
        type="button"
        data-testid="daily-probe-skip"
        class="daisy-btn"
        @click="skipDailyProbe"
      >
        Skip
      </button>
    </div>
  </div>
  <DailyProbe
    v-else-if="showDailyProbe"
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
