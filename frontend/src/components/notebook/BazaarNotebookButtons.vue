<template>
<div th:fragment="bazaarNotebookButtons(notebook)" class="btn-group btn-group-sm">
    <a
            th:unless="${notebook.skipReviewEntirely}"
            class="btn btn-sm" role="button" th:href="@{/subscriptions/notebooks/{id}/add_to_learning(id=${notebook.id})}" title="Add to my learning">
        <svg th:replace="_fragments/svgs :: add"/>
    </a>
</div>
</template>

<script setup>
  import NoteBreadcrumb from "../notes/NoteBreadcrumb.vue"
  const props = defineProps({ancestors: Array})
</script>
