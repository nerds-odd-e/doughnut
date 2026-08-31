<template>
  <div
    data-testid="daily-probe"
    class="h-full min-h-0 flex-1 flex flex-col text-center"
  >
    <template v-if="!finished">
      <div class="shrink-0 flex flex-col items-center gap-6 p-6">
        <p data-testid="daily-probe-instruction">
          {{ DAILY_PROBE_INSTRUCTION }}
        </p>
        <div
          data-testid="daily-probe-stimulus"
          class="text-7xl leading-none h-[1em]"
          :class="{ invisible: !stimulus }"
        >
          {{ stimulusArrow }}
        </div>
      </div>
      <div
        class="flex flex-1 min-h-24 w-full divide-x divide-base-300 pb-[max(0.75rem,env(safe-area-inset-bottom))]"
      >
        <div
          v-for="side in responseSides"
          :key="side"
          :data-testid="`daily-probe-response-zone-${side}`"
          class="flex-1 touch-none"
          :class="pressedSide === side ? 'bg-base-300' : 'bg-base-200'"
          @pointerdown="finishTrial(side)"
        />
      </div>
    </template>
    <div
      v-else
      class="flex-1 flex flex-col items-center justify-center gap-6 p-6"
    >
      <p
        v-if="speedText"
        data-testid="daily-probe-speed"
        class="text-2xl font-semibold"
      >
        {{ speedText }}
      </p>
      <p data-testid="daily-probe-accuracy" class="text-2xl font-semibold">
        {{ accuracyText }}
      </p>
      <p data-testid="daily-probe-lapses" class="text-2xl font-semibold">
        {{ lapseCount }}
      </p>
      <p
        v-if="variabilityText"
        data-testid="daily-probe-variability"
        class="text-2xl font-semibold"
      >
        {{ variabilityText }}
      </p>
      <p v-if="saveStatus === 'saved'" data-testid="daily-probe-saved">Saved</p>
      <button
        v-if="saveStatus === 'failed'"
        data-testid="daily-probe-retry"
        class="daisy-btn"
        @click="persistCompletedProbe"
      >
        Retry
      </button>
      <button
        data-testid="daily-probe-continue"
        class="daisy-btn daisy-btn-primary"
        :disabled="saveStatus !== 'saved'"
        @click="emit('complete')"
      >
        Continue
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  DAILY_PROBE_INSTRUCTION,
  DAILY_PROBE_ISI_MS,
  DAILY_PROBE_TIMEOUT_MS,
  dailyProbeAccuracy,
  dailyProbeLapseCount,
  dailyProbePracticeSequence,
  dailyProbeRunSequence,
  dailyProbeSpeed,
  dailyProbeVariability,
  mapDailyProbeKey,
  recordDailyProbeTrial,
  type DailyProbeSide,
  type DailyProbeTrial,
} from "@/models/dailyProbe"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import {
  computed,
  onActivated,
  onDeactivated,
  onMounted,
  onUnmounted,
  ref,
} from "vue"
import { useDailyProbePressFlash } from "./useDailyProbePressFlash"

const emit = defineEmits<{
  complete: []
}>()

const practiceCount = dailyProbePracticeSequence.length
const responseSides = [
  "left",
  "right",
] as const satisfies readonly DailyProbeSide[]

const trialIndex = ref(0)
const stimulus = ref<DailyProbeSide | undefined>()
const { pressedSide, startPressFlash, clearPressFlash } =
  useDailyProbePressFlash()
const scoredTrials = ref<DailyProbeTrial[]>([])
const finished = ref(false)
const saveStatus = ref<"unsaved" | "saved" | "failed">("unsaved")
const stimulusArrow = computed(() => {
  if (stimulus.value === "left") return "←"
  if (stimulus.value === "right") return "→"
  return ""
})

const speed = computed(() => dailyProbeSpeed(scoredTrials.value))
const accuracy = computed(() => dailyProbeAccuracy(scoredTrials.value))
const lapseCount = computed(() => dailyProbeLapseCount(scoredTrials.value))
const variability = computed(() => dailyProbeVariability(scoredTrials.value))
const speedText = computed(() => speed.value?.toFixed(2))
const accuracyText = computed(() => `${accuracy.value}%`)
const variabilityText = computed(() => variability.value?.toFixed(2))

let stimulusOnsetMs = 0
let respondedThisTrial = false
let scheduled: ReturnType<typeof setTimeout> | undefined
let abandoned = false

function clearScheduled() {
  if (scheduled !== undefined) {
    clearTimeout(scheduled)
    scheduled = undefined
  }
}

function startTrial() {
  respondedThisTrial = false
  stimulus.value = dailyProbeRunSequence[trialIndex.value]
  stimulusOnsetMs = Date.now()
  scheduled = setTimeout(() => finishTrial(), DAILY_PROBE_TIMEOUT_MS)
}

function finishTrial(response?: DailyProbeSide) {
  if (respondedThisTrial || finished.value) return
  respondedThisTrial = true
  clearScheduled()
  if (response !== undefined) startPressFlash(response)
  const current = dailyProbeRunSequence[trialIndex.value]!
  if (trialIndex.value >= practiceCount) {
    scoredTrials.value.push(
      recordDailyProbeTrial({
        stimulus: current,
        stimulusOnsetMs,
        ...(response === undefined ? {} : { response, responseMs: Date.now() }),
      })
    )
  }
  stimulus.value = undefined
  if (trialIndex.value === dailyProbeRunSequence.length - 1) {
    finished.value = true
    persistCompletedProbe()
    return
  }
  scheduled = setTimeout(() => {
    trialIndex.value += 1
    startTrial()
  }, DAILY_PROBE_ISI_MS)
}

async function persistCompletedProbe() {
  saveStatus.value = "unsaved"
  const { error } = await apiCallWithLoading(() =>
    DailyProbeController.createDailyProbe({
      body: {
        trials: scoredTrials.value,
        speed: speed.value,
        accuracy: accuracy.value,
        lapseCount: lapseCount.value,
        variability: variability.value,
      },
    })
  )
  saveStatus.value = !error ? "saved" : "failed"
}

function onKeydown(event: KeyboardEvent) {
  if (finished.value || !stimulus.value) return
  const response = mapDailyProbeKey(event.key)
  if (response === undefined) return
  event.preventDefault()
  finishTrial(response)
}

function attachKeyListener() {
  window.addEventListener("keydown", onKeydown)
}

function detachKeyListener() {
  window.removeEventListener("keydown", onKeydown)
}

function abandonUnfinishedRun() {
  clearScheduled()
  clearPressFlash()
  trialIndex.value = 0
  stimulus.value = undefined
  scoredTrials.value = []
  finished.value = false
  saveStatus.value = "unsaved"
  respondedThisTrial = false
  abandoned = true
}

onMounted(() => {
  attachKeyListener()
  startTrial()
})
onUnmounted(() => {
  detachKeyListener()
  clearScheduled()
  clearPressFlash()
})
onActivated(() => {
  attachKeyListener()
  if (!abandoned) return
  abandoned = false
  startTrial()
})
onDeactivated(() => {
  detachKeyListener()
  if (finished.value) return
  abandonUnfinishedRun()
})
</script>
