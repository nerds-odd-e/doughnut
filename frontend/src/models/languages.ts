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
    return this.language === Languages.ID ? this.note.noteContent.descriptionIDN : this.note.noteContent.description;
  }

  set description(description: string) {
    if (this.language === Languages.ID) {
      this.note.noteContent.descriptionIDN = description;
    } else {
      this.note.noteContent.description = description;
    }
  }

  get shortDescription (){
    return this.language === Languages.ID && this.note.shortDescriptionIDN ? this.note.shortDescriptionIDN : this.note.shortDescription
  }

  get  translationNoteAvailable() {
      return this.language === Languages.ID && !this.note.noteContent.titleIDN;
    }

  get  title() {
      if (!this.note.noteContent) return this.note.title;

      return this.language === Languages.ID
        ? this.note.noteContent.titleIDN
        : this.note.noteContent.title;
    }

  set title(title: string) {

      if (this.language === Languages.ID) {
        this.note.noteContent.titleIDN = title;
      } else {
        this.note.noteContent.title = title;
      }
    }

  get isTranslationOutdatedIDN() {
      if (this.language !== Languages.ID) {
        return false;
      } 
      if (!this.note.translationTextContent?.updatedAt) {
        return true;
      } 
      return new Date(this.note.textContent.updatedAt) > new Date(this.note.translationTextContent.updatedAt);
    }
}

export default Languages;
export { TranslatedNoteWrapper }