interface ViewType {
  value: string
  path: string
  title: string
  noteComponent: string
  redirectAfterDelete?: boolean
  fetchAll?: boolean
}

const viewTypes: Array<ViewType> = [
  {value: 'cards', path: 'cards', title: 'cards view', noteComponent: 'NoteCardsView', redirectAfterDelete: true},
  {value: 'article', path: 'article', title: 'article view', noteComponent: 'NoteArticleView', fetchAll: true},
  {value: 'mindmap', path: 'mindmap', title: 'mindmap view', noteComponent: 'NoteMindmapView', fetchAll: true},
]

const viewType = (value:string|undefined): ViewType => {
  const result = viewTypes.find((vt=> vt.value === value))
  if (result) return result
  return viewTypes[0]
}
export { viewTypes, viewType, ViewType}