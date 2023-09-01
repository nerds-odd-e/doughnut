type ViewTypeName = "cards";

const viewTypeNamesRaw = ["cards"];
const viewTypeNames = viewTypeNamesRaw as ViewTypeName[];

const sanitizeViewTypeName = (
  viewTypeString: string | undefined,
): ViewTypeName => {
  if (viewTypeString) {
    if (viewTypeNamesRaw.includes(viewTypeString))
      return viewTypeString as ViewTypeName;
  }
  return "cards";
};

const routeNameForViewType = () => {
  return "noteShow";
};

export { viewTypeNames, sanitizeViewTypeName, routeNameForViewType };
export type { ViewTypeName };
