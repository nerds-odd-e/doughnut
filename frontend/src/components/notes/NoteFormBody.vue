<template>
    <TextInput scopeName='note' field='title' v-model="modelValue.title"/>
    <TextArea scopeName='note' field='description' v-model="modelValue.description"/>
    <ImageInput scopeName='note' field='uploadPicture' placeholder="Optional. upload own picture." v-model="modelValue.uploadPictureProxy"/>
    <TextInput scopeName='note' field='pictureUrl' placeholder="Full url of existing picture." v-model="modelValue.pictureUrl"/>
    <CheckInput scopeName='note' field='useParentPicture' v-model="modelValue.useParentPicture"/>
    <TextInput scopeName='note' field='pictureMask' v-model="modelValue.pictureMask"/>
    <TextInput scopeName='note' field='url' v-model="modelValue.url"/>
    <CheckInput scopeName='note' field='urlIsVideo' v-model="modelValue.urlIsVideo"/>
    <CheckInput scopeName='note' field='hideTitleInArticle' v-model="modelValue.hideTitleInArticle"/>
    <CheckInput scopeName='note' field='showAsBulletInArticle' v-model="modelValue.showAsBulletInArticle"/>
    <CheckInput scopeName='note' field='skipReview' v-model="modelValue.skipReview"/>
</template>

<script setup>
import TextInput from "../form/TextInput.vue"
import CheckInput from "../form/CheckInput.vue"
import TextArea from "../form/TextArea.vue"
import ImageInput from "../form/ImageInput.vue"

const props = defineProps({modelValue: {type: Object, required: true }})
</script>
