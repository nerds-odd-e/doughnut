import submittableForm from "../../submittableForm"

const noteCreationForm = {
  createNote: (topic: string, linkTypeToParent?: string, wikidataId?: string) => {
    submittableForm.submitWith({
      Topic: topic,
      "Link Type To Parent": linkTypeToParent,
      "Wikidata Id": wikidataId,
    })
  },
  createNoteWithAttributes(attributes: Record<string, string>) {
    const {
      Topic,
      ["Link Type To Parent"]: linkTypeToParent,
      ["Wikidata Id"]: wikidataId,
      ...remainingAttrs
    } = attributes
    expect(Object.keys(remainingAttrs)).to.have.lengthOf(0)
    return this.createNote(Topic!, linkTypeToParent, wikidataId)
  },
}

export default noteCreationForm
