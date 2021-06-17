<template>
<div class="btn-group btn-group-sm">
    <a v-if="!notebook.skipReviewEntirely"
            class="btn btn-sm" role="button" :href="`/subscriptions/notebooks/${notebook.id}/add_to_learning`" title="Add to my learning">
        <SvgAdd/>
    </a>
</div>
</template>

<script setup>
  import SvgAdd from "../svgs/SvgAdd.vue"
  const props = defineProps({notebook: Object})
</script>
