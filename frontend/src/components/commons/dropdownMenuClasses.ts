export type DropdownMenuSize = "narrow" | "wide" | "history"

const dropdownMenuPanelBaseClass =
  "daisy-dropdown-content daisy-menu bg-base-100 rounded-box p-2 shadow z-[1000]"

export function dropdownMenuPanelClass(
  size: DropdownMenuSize = "narrow"
): string {
  switch (size) {
    case "wide":
      return `${dropdownMenuPanelBaseClass} min-w-[16rem] w-[17.5rem] max-w-[17.5rem]`
    case "history":
      return `${dropdownMenuPanelBaseClass} min-w-[12rem] max-w-[20rem] max-h-60 overflow-y-auto`
    default:
      return `${dropdownMenuPanelBaseClass} w-52`
  }
}

export const dropdownMenuItemClass = "daisy-menu-item p-0"

export const dropdownMenuButtonClass =
  "daisy-btn daisy-btn-ghost h-auto min-h-0 w-full justify-start gap-2 py-2 font-normal"

export const dropdownMenuButtonMultilineClass = `${dropdownMenuButtonClass} whitespace-normal items-start text-left`
