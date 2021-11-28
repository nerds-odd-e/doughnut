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
}

export default Languages;
export { TranslatedNoteWrapper }