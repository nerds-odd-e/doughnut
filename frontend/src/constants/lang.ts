interface ILanguages {
    [property: string]: string
}

const Languages: ILanguages = {
    ID: "ID",
    EN: "EN"
};

class NoteWrapper {
  note: any
  constructor(note: any) {
    this.note = note;
  }
  translatedDescription(language: string){
    return language === Languages.ID && this.note.noteContent && this.note.noteContent.descriptionIDN ? this.note.noteContent.descriptionIDN : this.note.noteContent.description;
  }
  translatedShortDescription =(language: string) => {
    return language === Languages.ID && this.note.shortDescriptionIDN ? this.note.shortDescriptionIDN : this.note.shortDescription;
  }
}

export default Languages;
export { NoteWrapper }