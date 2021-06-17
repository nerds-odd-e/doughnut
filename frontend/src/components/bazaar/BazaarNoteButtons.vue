<template>
    <BazaarNotebookButtons :notebook="notebook" />
    <a role="button" class="btn btn-sm" title="Article View" :href="`/bazaar/articles/${note.id}`">
        <SvgArticle/>
    </a>
</template>

<script setup>
  import SvgArticle from "../svgs/SvgArticle.vue"
  import BazaarNotebookButtons from "../notebook/BazaarNotebookButtons.vue"
  const props = defineProps({note: Object, notebook: Object})
</script>
