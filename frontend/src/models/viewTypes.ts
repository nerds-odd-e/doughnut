interface ViewType {
  value: string
  path: string
  title: string
  redirectAfterDelete?: boolean
  fetchAll?: boolean
}

const viewTypes: Array<ViewType> = [
  {value: 'cards', path: 'cards', title: 'cards view', redirectAfterDelete: true},
  {value: 'article', path: 'article', title: 'article view', fetchAll: true},
  {value: 'mindmap', path: 'mindmap', title: 'mindmap view', fetchAll: true},
]

const viewType = (value:string|undefined): ViewType => {
  const result = viewTypes.find((vt=> vt.value === value))
  if (result) return result
  return viewTypes[0]
}
export { viewTypes, viewType, ViewType}