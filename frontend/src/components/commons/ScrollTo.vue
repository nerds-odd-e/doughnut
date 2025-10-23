<template>
  <mark ref="scrollRef" />
</template>

<script setup lang="ts">
import type { Ref } from "vue"
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = defineProps<{
  scrollTrigger?: number | string
}>()

const scrollRef: Ref<HTMLElement | null> = ref(null)

let observer: IntersectionObserver | null = null

onMounted(async () => {
  await nextTick()

  const handleIntersection = (entries: IntersectionObserverEntry[]) => {
    if (entries[0] && entries[0].isIntersecting === false) {
      scrollRef.value?.scrollIntoView({ behavior: "smooth" })
    }
    observer?.disconnect()
  }

  observer = new IntersectionObserver(handleIntersection, {
    root: null, // Use the viewport as the root
    threshold: 0, // Trigger when the element is not visible at all
  })

  if (scrollRef.value) {
    observer.observe(scrollRef.value)
  }
})

onBeforeUnmount(() => {
  if (scrollRef.value) {
    observer?.unobserve(scrollRef.value)
  }
})

watch(
  () => props.scrollTrigger,
  () => {
    if (scrollRef.value) {
      scrollRef.value.scrollIntoView({ behavior: "smooth" })
    }
  }
)
</script>

<style scoped>
mark {
  display: block;
  width: 0;
  height: 0;
  visibility: hidden;
}
</style>
