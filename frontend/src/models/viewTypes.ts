type ViewTypeName = "cards" | "article" | "mindmap";

const viewTypeNamesRaw = ["cards", "article", "mindmap"];
const viewTypeNames = viewTypeNamesRaw as ViewTypeName[];

const sanitizeViewTypeName = (
  viewTypeString: string | undefined
): ViewTypeName => {
  if (viewTypeString) {
    if (viewTypeNamesRaw.includes(viewTypeString))
      return viewTypeString as ViewTypeName;
  }
  return "cards";
};

export { viewTypeNames, sanitizeViewTypeName };
export type { ViewTypeName };
