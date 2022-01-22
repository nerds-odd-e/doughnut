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
    return this.language === Languages.ID ? this.note.translationTextContent?.description : this.note.textContent.description;
  }

  set description(description: string) {
    if (this.language === Languages.ID) {
      this.note.noteContent.descriptionIDN = description;
      if(!this.note.translationTextContent) {
        this.note.translationTextContent = {}
      }
      this.note.translationTextContent.description = description;
    } else {
      this.note.noteContent.description = description;
      this.note.textContent.description = description;
    }
  }

  get shortDescription (){
    const str = this.description
    const num = 50
    if (str.length > num) {
      return str.slice(0, num) + "...";
    }
    return str;
  }

  get  translationNoteAvailable() {
      return this.language === Languages.ID && !this.note.noteContent.titleIDN;
    }

  get  title() {
      if (!this.note.noteContent) return this.note.title;

      return this.language === Languages.ID
        ? this.note.translationTextContent?.title
        : this.note.textContent.title;
    }

  set title(title: string) {

      if (this.language === Languages.ID) {
        this.note.noteContent.titleIDN = title;
        if(!this.note.translationTextContent) {
          this.note.translationTextContent = {}
        }
        this.note.translationTextContent.title = title;
      } else {
        this.note.noteContent.title = title;
        this.note.textContent.title = title;
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