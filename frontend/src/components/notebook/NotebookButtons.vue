<template>
<div class="btn-group btn-group-sm">
    <a role="button" class="btn btn-sm" title="Article View"
       :href="`/notes/articles/${notebook.headNote.id}`">
        <SvgArticle/>
    </a>
    <a class="btn btn-sm" role="button" :href="`/notebooks/${notebook.id}/edit`"
       title="edit notebook settings">
        <SvgEditNotebook/>
    </a>
    <form :action="`/notebooks/${notebook.id}/share`" method="post"
          onsubmit="return confirm('Are you sure to share?')">
        <button class="btn btn-sm" title="Share notebook to bazaar">
            <SvgBazaarShare/>
        </button>
    </form>
</div>
</template>

<script>
import SvgArticle from "../svgs/SvgArticle.vue"
import SvgEditNotebook from "../svgs/SvgEditNotebook.vue"
import SvgBazaarShare from "../svgs/SvgBazaarShare.vue"
export default {
  name: 'NotebookCardWithHeaderAndButtons',
  props: { notebook: Object },
  components: { SvgArticle, SvgEditNotebook, SvgBazaarShare },
}
</script>
