<template>
<div class="btn-group btn-group-sm">
    <slot name="additional-buttons"/>
    <a role="button" class="btn btn-sm" title="Article View"
       :href="`/notes/articles/${notebook.headNote.id}`">
        <SvgArticle/>
    </a>
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
import SvgArticle from "../svgs/SvgArticle.vue"
import NotebookEditButton from "./NotebookEditButton.vue"
import SvgEditNotebook from "../svgs/SvgEditNotebook.vue"
import SvgBazaarShare from "../svgs/SvgBazaarShare.vue"
const props = defineProps({ notebook: Object })
</script>
