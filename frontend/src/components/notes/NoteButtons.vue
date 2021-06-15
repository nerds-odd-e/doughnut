<template>
<div class="btn-group btn-group-sm">
    <a class="btn btn-sm" :href="`/notes/${note.id}/edit`" role="button" title="edit note">
        <SvgEdit/>
    </a>
    <a role="button" class="btn btn-sm" title="Cards View" :href="`/notes/articles/${note.id}`">
        <SvgArticle/>
    </a>
    <a class="btn btn-sm" :href="`/links/${note.id}/link`" role="button" title="link note">
        <SvgLinkNote/>
    </a>
    <a class="btn btn-light dropdown-toggle"
            data-toggle="dropdown" aria-haspopup="true"
            aria-expanded="false" role="button" title="more options">
        <SvgCog/>
    </a>
    <div class="dropdown-menu dropdown-menu-right">
        <a  class="dropdown-item" :href="`/notes/${note.id}/review_setting`" title="edit review settings">
            <SvgReviewSetting/>
            Edit review settings
        </a>
        <form :action="`/notes/${note.id}/delete`" method="post"
                      onsubmit="return confirm('Are you sure to delete?')">
            <button class="dropdown-item" title="delete note">
                <SvgRemove/>
                Delete
            </button>
        </form>
    </div>
</div>
</template>

<script setup>
  import SvgEdit from "../svgs/SvgEdit.vue"
  import SvgArticle from "../svgs/SvgArticle.vue"
  import SvgLinkNote from "../svgs/SvgLinkNote.vue"
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
  import SvgRemove from "../svgs/SvgRemove.vue"
  const props = defineProps({note: Object})
</script>
