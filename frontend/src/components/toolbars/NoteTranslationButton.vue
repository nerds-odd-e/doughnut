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
        note: Object,
    },
    data(){
        return {
            currentLang: Languages.EN,
            translationLang: Languages.ID,
        }
    },
    mounted(){
        let currentLanguage = this.$store?.getters.getCurrentLanguage();
        if (currentLanguage === Languages.ID) {
            this.currentLang = Languages.ID;
            this.translationLang = Languages.EN;
        }

        this.changeButtonTranslation(this.currentLang, this.translationLang);
    },
    methods: {
        changeButtonTranslation(targetLanguage, translationLang){
            this.currentLang = targetLanguage;
            this.translationLang = translationLang;
        },
        toggleLanguage(){
            if (this.currentLang === Languages.EN) {
                this.changeButtonTranslation(Languages.ID, Languages.EN);
                this.$store?.commit('changeNotesLanguage', Languages.ID);
            } else {
                this.changeButtonTranslation(Languages.EN, Languages.ID);
                this.$store?.commit('changeNotesLanguage', Languages.EN);
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