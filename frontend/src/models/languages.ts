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
      this.note.noteAccessories.descriptionIDN = description;
      if(!this.note.translationTextContent) {
        this.note.translationTextContent = {}
      }
      this.note.translationTextContent.description = description;
    } else {
      this.note.noteAccessories.description = description;
      this.note.textContent.description = description;
    }
  }

  get shortDescription (){
    const num = 50
    if (this.description.length > num) {
      return `${this.description.slice(0, num)  }...`;
    }
    return this.description;
  }

  get  translationNoteAvailable() {
      return this.language === Languages.ID && !this.note.noteAccessories.titleIDN;
    }

  get  title() {
      if (!this.note.noteAccessories) return this.note.title;

      return this.language === Languages.ID
        ? this.note.translationTextContent?.title
        : this.note.textContent.title;
    }

  set title(title: string) {

      if (this.language === Languages.ID) {
        this.note.noteAccessories.titleIDN = title;
        if(!this.note.translationTextContent) {
          this.note.translationTextContent = {}
        }
        this.note.translationTextContent.title = title;
      } else {
        this.note.noteAccessories.title = title;
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