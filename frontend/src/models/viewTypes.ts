type ViewTypeName = "cards" | "article" | "mindmap";

interface ViewType {
  value: ViewTypeName;
  title: string;
}

const viewTypeNames = ["cards", "article", "mindmap"] as ViewTypeName[];

const viewTypes: Array<ViewType> = [
  {
    value: "cards",
    title: "cards view",
  },
  { value: "article", title: "article view" },
  { value: "mindmap", title: "mindmap view" },
];

const viewType = (value: string | undefined): ViewType => {
  const result = viewTypes.find((vt) => vt.value === value);
  if (result) return result;
  return viewTypes[0];
};
export { viewTypes, viewType, ViewType, ViewTypeName, viewTypeNames };
