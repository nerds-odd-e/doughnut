"""Markdown pieces the report sections share."""


def table(header, rows):
    lines = ["| " + " | ".join(header) + " |", "| " + " | ".join("---" for _ in header) + " |"]
    lines += ["| " + " | ".join(str(c) for c in row) + " |" for row in rows]
    return "\n".join(lines)


def band_label(start, end):
    return f"{start:02d}:00–{end - 1:02d}:59"


def share(part, whole):
    return f"{part} of {whole} ({100 * part / whole:.0f}%)" if whole else f"{part} of 0"
