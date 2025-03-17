<template>
  <button
    class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-gap-2"
    role="button"
    title="Wiki Association"
    @click="onClickWikidata"
  >
    <SvgAssociation />
    Go to Wikidata
  </button>
</template>

<script setup lang="ts">
import SvgAssociation from "@/components/svgs/SvgAssociation.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import nonBlockingPopup from "@/managedApi/window/nonBlockingPopup"

const props = defineProps<{
  wikidataId: string
}>()

const { managedApi } = useLoadingApi()

const getWikidataItem = async () => {
  return (
    await managedApi.restWikidataController.fetchWikidataEntityDataById(
      props.wikidataId
    )
  ).WikipediaEnglishUrl
}

const wikiUrl = async () => {
  const wikipediaEnglishUrl = await getWikidataItem()
  if (wikipediaEnglishUrl !== "") {
    return wikipediaEnglishUrl
  }
  return `https://www.wikidata.org/wiki/${props.wikidataId}`
}

const onClickWikidata = () => {
  nonBlockingPopup(wikiUrl())
}
</script>
