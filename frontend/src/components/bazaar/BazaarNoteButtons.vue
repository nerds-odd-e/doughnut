<template>
    <BazaarNotebookButtons :notebook="notebook" />
    <a role="button" class="btn btn-sm" title="Article View" :href="`/bazaar/articles/${note.id}`">
        <SvgArticle/>
    </a>
</template>

<script>
  import SvgArticle from "../svgs/SvgArticle.vue"
  import BazaarNotebookButtons from "../notebook/BazaarNotebookButtons.vue"

  export default {
    props: {note: Object, notebook: Object},
    components: { SvgArticle, BazaarNotebookButtons}
  }
</script>
