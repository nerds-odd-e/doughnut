<template>
<div class="btn-group btn-group-sm">
    <slot name="additional-buttons"/>
    <NotebookEditButton v-bind="{notebook}"/>
    <form :action="`/notebooks/${notebook.id}/share`" method="post"
          onsubmit="return confirm('Are you sure to share?')">
        <button class="btn btn-sm" title="Share notebook to bazaar">
            <SvgBazaarShare/>
        </button>
    </form>
</div>
</template>

<script setup>
import NotebookEditButton from "./NotebookEditButton.vue"
import SvgBazaarShare from "../svgs/SvgBazaarShare.vue"
const props = defineProps({ notebook: Object })
</script>
