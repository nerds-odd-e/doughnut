<template>
  <div
    data-testid="daily-probe"
    class="h-full flex flex-col items-center justify-center gap-6 p-6 text-center"
  >
    <template v-if="!finished">
      <p>{{ DAILY_PROBE_INSTRUCTION }}</p>
      <div
        v-if="stimulus"
        data-testid="daily-probe-stimulus"
        class="text-7xl leading-none"
      >
        {{ stimulus === "left" ? "←" : "→" }}
      </div>
    </template>
    <template v-else>
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
    </template>
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
  dailyProbeScoredSequence,
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

const emit = defineEmits<{
  complete: []
}>()

const runSequence: readonly DailyProbeSide[] = [
  ...dailyProbePracticeSequence,
  ...dailyProbeScoredSequence,
]
const practiceCount = dailyProbePracticeSequence.length

const trialIndex = ref(0)
const stimulus = ref<DailyProbeSide | undefined>()
const scoredTrials = ref<DailyProbeTrial[]>([])
const finished = ref(false)
const saveStatus = ref<"unsaved" | "saved" | "failed">("unsaved")

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
  stimulus.value = runSequence[trialIndex.value]
  stimulusOnsetMs = Date.now()
  scheduled = setTimeout(() => finishTrial(), DAILY_PROBE_TIMEOUT_MS)
}

function finishTrial(key?: string) {
  if (respondedThisTrial || finished.value) return
  respondedThisTrial = true
  clearScheduled()
  const current = runSequence[trialIndex.value]!
  if (trialIndex.value >= practiceCount) {
    scoredTrials.value.push(
      recordDailyProbeTrial({
        stimulus: current,
        stimulusOnsetMs,
        ...(key === undefined ? {} : { key, responseMs: Date.now() }),
      })
    )
  }
  stimulus.value = undefined
  if (trialIndex.value === runSequence.length - 1) {
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
  if (mapDailyProbeKey(event.key) === undefined) return
  event.preventDefault()
  finishTrial(event.key)
}

function attachKeyListener() {
  window.addEventListener("keydown", onKeydown)
}

function detachKeyListener() {
  window.removeEventListener("keydown", onKeydown)
}

function abandonUnfinishedRun() {
  clearScheduled()
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
