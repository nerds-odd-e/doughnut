type ViewTypeName = "cards" | "article" | "mindmap";

interface ViewType {
  value: ViewTypeName;
  path: string;
  title: string;
}

const viewTypes: Array<ViewType> = [
  {
    value: "cards",
    path: "cards",
    title: "cards view",
  },
  { value: "article", path: "article", title: "article view" },
  { value: "mindmap", path: "mindmap", title: "mindmap view" },
];

const viewType = (value: string | undefined): ViewType => {
  const result = viewTypes.find((vt) => vt.value === value);
  if (result) return result;
  return viewTypes[0];
};
export { viewTypes, viewType, ViewType, ViewTypeName };
