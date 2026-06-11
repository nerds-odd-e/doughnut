<template>
  <button
    class="daisy-btn daisy-btn-sm daisy-btn-ghost gap-2"
    role="button"
    title="Wiki Association"
    @click="onClickWikidata"
  >
    <Link2 class="w-6 h-6" />
    Go to Wikidata
  </button>
</template>

<script setup lang="ts">
import { Link2 } from "@lucide/vue"
import { openWikidataEntityBrowseUrlInNonBlockingPopup } from "@/utils/wikidataEntityBrowseUrl"

const props = defineProps<{
  wikidataId: string
}>()

const onClickWikidata = () => {
  openWikidataEntityBrowseUrlInNonBlockingPopup(props.wikidataId)
}
</script>
