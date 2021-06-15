<template>
<div class="btn-group btn-group-sm">
    <a class="btn btn-sm" :href="`/notes/${note.id}/edit`" role="button" title="edit note">
        <SvgEdit/>
    </a>
    <!-- <a role="button" class="btn btn-sm" title="Cards View" th:href="@{/notes/articles/{id}(id=${note.id})}">
        <svg th:replace="_fragments/svgs :: article"/>
    </a>
    <a class="btn btn-sm" th:href="@{/links/{id}/link(id=${note.id})}" role="button" title="link note">
        <svg th:replace="_fragments/svgs :: linkNote"/>
    </a>
    <a class="btn btn-light dropdown-toggle"
            data-toggle="dropdown" aria-haspopup="true"
            aria-expanded="false" role="button" title="more options">
        <svg th:replace="_fragments/svgs :: cog"/>
    </a>
    <div class="dropdown-menu dropdown-menu-right">
        <a  class="dropdown-item" th:href="@{/notes/{id}/review_setting(id=${note.id})}"
           title="edit review settings">
            <svg th:replace="_fragments/svgs :: reviewSetting"/>
            Edit review settings
        </a>
        <form th:action="@{/notes/{id}/delete(id=${note.id})}" th:method="delete"
                      onsubmit="return confirm('Are you sure to delete?')">
            <button class="dropdown-item" title="delete note">
                <svg th:replace="_fragments/svgs :: remove"/>
                Delete
            </button>
        </form>
    </div> -->
</div>
</template>

<script setup>
  import SvgEdit from "../svgs/SvgEdit.vue"
  const props = defineProps({note: Object})
</script>
