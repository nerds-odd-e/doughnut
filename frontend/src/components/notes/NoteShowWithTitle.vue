<template>
    <div th:object="${note.noteContent}">
        <slot></slot>
        <div v-if="!!note.noteContent.url">
            <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
            <label v-else>Url:</label>
            <a :href="note.noteContent.url">{{note.noteContent.url}}</a>
        </div>
    </div>
    <ShowPicture :note="note" :opacity="0.2"/>
    <!-- <ul>
        <li th:each="linkType:${note.linkTypes(currentUser?.entity)}">
            <span th:text="${linkType.label}"/>
            <span class="badge badge-light mr-1" th:each="link:${note.linksOfTypeThroughDirect(linkType)}">
                <span th:unless="${forBazaar}" th:remove="tag">
                    <span th:replace=":: linkWithHtmlLink(${link}, ${link.targetNote})"/>
                </span>
                <a th:href="'/bazaar/notes/' + ${link.targetNote.id}" th:text="${link.targetNote.noteContent.title}"
                   th:if="${forBazaar}"/>
            </span>
            <span class="badge badge-warning mr-1"
                  th:each="link:${note.linksOfTypeThroughReverse(linkType, currentUser?.entity)}">
                <span th:unless="${forBazaar}" th:remove="tag">
                    <span th:replace=":: linkWithHtmlLink(${link}, ${link.sourceNote})"/>
                </span>
                <a th:href="'/bazaar/notes/' + ${link.sourceNote.id}" th:text="${link.sourceNote.noteContent.title}" th:if="${forBazaar}"/>
            </span>
        </li>
    </ul> -->
</template>

<script setup>
  import ShowPicture from "./ShowPicture.vue"
  const props = defineProps({note: Object, level: Number, forBazaar: Boolean})
</script>
