import submittableForm from '../../submittableForm'

const noteCreationForm = {
  createNote: (topic: string, wikidataId?: string) => {
    submittableForm.submitWith({
      Topic: topic,
      'Wikidata Id': wikidataId,
    })
  },
  createNoteWithAttributes(attributes: Record<string, string>) {
    const { Topic, 'Wikidata Id': wikidataId, ...remainingAttrs } = attributes
    expect(Object.keys(remainingAttrs)).to.have.lengthOf(0)
    return this.createNote(Topic!, wikidataId)
  },
}

export default noteCreationForm
