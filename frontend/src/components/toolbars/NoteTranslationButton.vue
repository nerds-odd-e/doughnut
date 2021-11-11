<template name="NoteTranslationButton">
    <button 
        class="btn btn-small btn-translation" 
        :lang="currentLang" 
        :title="`Translate to ${translationLang}`"
        @click="toggleLanguage()"
        >
        <component v-bind:is="`SvgFlag${translationLang}`" :class="`flag flag-${translationLang}`"></component>
    </button>
</template>

<script>
import SvgFlagID from "../svgs/flags/SvgFlagID.vue";
import SvgFlagEN from "../svgs/flags/SvgFlagEN.vue";
import Languages from "../../constants/lang";

export default {
    name: "NoteTranslationButton",
    components: {
        SvgFlagID,
        SvgFlagEN,
    },
    props: {
        noteId: Number,
    },
    data(){
        return {
            currentLang: Languages.EN,
            translationLang: Languages.ID,
        }
    },
    methods: {
        changeLanguage(targetLanguage, translationLang){
            this.currentLang = targetLanguage;
            this.translationLang = translationLang;
        },
        toggleLanguage(){
            if (this.currentLang === Languages.EN) {
                this.changeLanguage(Languages.ID, Languages.EN);
                this.$store.commit("changeNotesLanguage", { id: this.noteId, language: Languages.ID });
            } else {
                this.changeLanguage(Languages.EN, Languages.ID);
                this.$store.commit("changeNotesLanguage", { id: this.noteId, language: Languages.EN });
            }            
        }
    }
}
</script>

<style lang="scss" scoped>
    .btn-translation svg {
        border: 1px solid grey;
        max-width: 45px;
    }
</style>