<template>
<div class="btn-group btn-group-sm">
    <a v-if="!notebook.skipReviewEntirely"
            class="btn btn-sm" role="button" :href="`/subscriptions/notebooks/${notebook.id}/add_to_learning`" title="Add to my learning">
        <SvgAdd/>
    </a>
</div>
</template>

<script>
  import SvgAdd from "../svgs/SvgAdd.vue"
  export default {
    name: "BazaarNotebookButtons",
    props: {notebook: Object},
    components: { SvgAdd }

  }
</script>
