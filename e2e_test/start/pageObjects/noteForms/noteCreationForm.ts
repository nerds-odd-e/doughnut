import submittableForm from "../../submittableForm"

const noteCreationForm = {
  createNote: (topic: string, linkTypeToParent: string, wikidataId: string) => {
    submittableForm.submitWith({
      Topic: topic,
      "Link Type To Parent": linkTypeToParent,
      "Wikidata Id": wikidataId,
    })
  },
}

export default noteCreationForm
