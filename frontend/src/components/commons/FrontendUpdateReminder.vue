<template>
  <div
    v-if="updateAvailable"
    class="daisy-alert daisy-alert-info fixed bottom-4 right-4 z-[10001] w-auto"
    data-testid="frontend-update-reminder"
  >
    <span>A newer version of Donut is available.</span>
    <button class="daisy-btn daisy-btn-sm" @click="browserLocation.reload()">
      Reload
    </button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue"
import { browserLocation } from "@/managedApi/window/browserLocation"

const updateAvailable = ref(false)

const moduleEntries = (doc: Document) =>
  Array.from(doc.querySelectorAll('script[type="module"][src]'), (script) =>
    script.getAttribute("src")
  ).join(" ")

const checkForUpdate = async () => {
  if (document.visibilityState !== "visible") return
  const response = await fetch("/", { cache: "no-store" })
  const served = new DOMParser().parseFromString(
    await response.text(),
    "text/html"
  )
  updateAvailable.value = moduleEntries(served) !== moduleEntries(document)
}

onMounted(() => document.addEventListener("visibilitychange", checkForUpdate))
onUnmounted(() =>
  document.removeEventListener("visibilitychange", checkForUpdate)
)
</script>
