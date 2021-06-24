<template>
    <TextInput scopeName='note' field='title' v-model="modelValue.title"/>
    <TextArea scopeName='note' field='description' v-model="modelValue.description"/>
    <div th:replace="_fragments/forms :: imageInput('note', 'uploadPicture', 'Optional. upload own picture.')"/>
    <div th:replace="_fragments/forms :: textInput('note', 'pictureUrl', 'Full url of existing picture.', false)"/>
    <div th:replace="_fragments/forms :: checkInput('note', 'useParentPicture')"/>
    <div th:replace="_fragments/forms :: textInput('note', 'pictureMask', null, false)"/>
    <div th:replace="_fragments/forms :: textInput('note', 'url', null, false)"/>
    <div th:replace="_fragments/forms :: checkInput('note', 'urlIsVideo')"/>
    <div th:replace="_fragments/forms :: checkInput('note', 'hideTitleInArticle')"/>
    <div th:replace="_fragments/forms :: checkInput('note', 'showAsBulletInArticle')"/>
    <div th:replace="_fragments/forms :: checkInput('note', 'skipReview')"/>
</template>

<script setup>
import TextInput from "../form/TextInput.vue"
import TextArea from "../form/TextArea.vue"
import { ref, defineProps } from "vue"

const props = defineProps({modelValue: {type: Object, required: true }})
const v=ref("abc")
</script>
