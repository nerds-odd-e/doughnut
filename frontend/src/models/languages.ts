interface ILanguages {
    [property: string]: string
}

const Languages: ILanguages = {
    ID: "ID",
    EN: "EN"
};

class TranslatedNoteWrapper {
  note: any

  language: string

  constructor(note: any, language: string) {
    this.note = note;
    this.language = language
  }

  get description(){
    return this.language === Languages.ID && this.note.noteContent && this.note.noteContent.descriptionIDN ? this.note.noteContent.descriptionIDN : this.note.noteContent.description;
  }

  get shortDescription (){
    return this.language === Languages.ID && this.note.shortDescriptionIDN ? this.note.shortDescriptionIDN : this.note.shortDescription
  }

  get  translationNoteAvailable() {
      return this.language === Languages.ID && !this.note.noteContent.titleIDN;
    }

  get  title() {
      if (!this.note.noteContent) return this.note.title;

      return this.language === Languages.ID &&
        this.note.noteContent.titleIDN
        ? this.note.noteContent.titleIDN
        : this.note.noteContent.title;
    }

  get isTranslationOutdatedIDN() {
      return this.language === Languages.ID && this.note.noteContent.isTranslationOutdatedIDN;
    }

  set title(title: string) {

    if (this.language === Languages.ID) {
      this.note.noteContent.titleIDN = title;
    } else {
      this.note.noteContent.title = title;
    }
  }
}

export default Languages;
export { TranslatedNoteWrapper }