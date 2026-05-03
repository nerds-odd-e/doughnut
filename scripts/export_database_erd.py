#!/usr/bin/env python3
"""Write docs/database-erd.md from MySQL information_schema (FKs + key columns)."""

from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT = REPO_ROOT / "docs" / "database-erd.md"

SCHEMA_CANDIDATES = (
    "doughnut_development",
    "doughnut_test",
    "doughnut_e2e_test",
)


def _env(name: str, default: str) -> str:
    return os.environ.get(name, default)


def _mysql_base_cmd() -> list[str]:
    return [
        "mysql",
        "-h",
        _env("DOUGHNUT_ERD_MYSQL_HOST", "127.0.0.1"),
        "-P",
        _env("DOUGHNUT_ERD_MYSQL_PORT", "3309"),
        "-u",
        _env("DOUGHNUT_ERD_MYSQL_USER", "doughnut"),
        f"-p{_env('DOUGHNUT_ERD_MYSQL_PASSWORD', 'doughnut')}",
    ]


def _run_mysql(sql: str, database: str | None = None) -> str:
    cmd = _mysql_base_cmd()
    if database:
        cmd.append(database)
    cmd.extend(["-B", "-N", "-e", sql])
    proc = subprocess.run(
        cmd,
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
    )
    if proc.returncode != 0:
        msg = proc.stderr.strip() or proc.stdout.strip() or f"exit {proc.returncode}"
        raise RuntimeError(msg)
    return proc.stdout


def _valid_schema(name: str) -> bool:
    return bool(re.fullmatch(r"[A-Za-z0-9_]+", name))


def _table_count(schema: str) -> int:
    out = _run_mysql(
        "SELECT COUNT(*) FROM information_schema.tables "
        f"WHERE table_schema = '{schema}' AND table_type = 'BASE TABLE'",
    )
    return int(out.strip())


def _pick_schema() -> str:
    explicit = os.environ.get("DOUGHNUT_ERD_SCHEMA", "").strip()
    if explicit:
        if not _valid_schema(explicit):
            sys.exit("DOUGHNUT_ERD_SCHEMA must be alphanumeric plus underscores only")
        if _table_count(explicit) == 0:
            sys.exit(
                f"No tables in schema {explicit!r}. Start MySQL, migrate, or set DOUGHNUT_ERD_SCHEMA.",
            )
        return explicit

    for schema in SCHEMA_CANDIDATES:
        if not _valid_schema(schema):
            continue
        if _table_count(schema) > 0:
            print(f"export_database_erd: using schema {schema}", file=sys.stderr)
            return schema

    sys.exit(
        "No Flyway-populated schema found (tried doughnut_development, doughnut_test, doughnut_e2e_test). "
        "Start MySQL on 3309 and run migrations, or set DOUGHNUT_ERD_SCHEMA.",
    )


def _parse_tsv_rows(text: str) -> list[list[str]]:
    rows: list[list[str]] = []
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue
        rows.append(line.split("\t"))
    return rows


def _q(name: str) -> str:
    if name in ("user", "note"):
        return '"' + name + '"'
    if re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", name):
        return name
    return '"' + name.replace('"', '\\"') + '"'


def _mermaid_type(ctype: str) -> str:
    ct_lower = ctype.lower()
    if "bigint" in ct_lower:
        return "bigint"
    if "int" in ct_lower and "point" not in ct_lower:
        return "int"
    if "char" in ct_lower or "text" in ct_lower or "blob" in ct_lower or "binary" in ct_lower:
        return "string"
    if "float" in ct_lower or "double" in ct_lower or "decimal" in ct_lower:
        return "float"
    if "date" in ct_lower or "time" in ct_lower:
        return "datetime"
    return "string"


def main() -> None:
    schema = _pick_schema()

    cols_sql = (
        "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_KEY "
        "FROM information_schema.COLUMNS "
        f"WHERE TABLE_SCHEMA = '{schema}' "
        "ORDER BY TABLE_NAME, ORDINAL_POSITION"
    )
    fk_sql = (
        "SELECT k.TABLE_NAME, k.COLUMN_NAME, k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME "
        "FROM information_schema.KEY_COLUMN_USAGE k "
        "JOIN information_schema.REFERENTIAL_CONSTRAINTS r "
        "ON k.CONSTRAINT_SCHEMA = r.CONSTRAINT_SCHEMA AND k.CONSTRAINT_NAME = r.CONSTRAINT_NAME "
        f"WHERE k.TABLE_SCHEMA = '{schema}' AND k.REFERENCED_TABLE_NAME IS NOT NULL "
        "ORDER BY k.TABLE_NAME, k.COLUMN_NAME"
    )

    col_rows = _parse_tsv_rows(_run_mysql(cols_sql))
    fk_rows = _parse_tsv_rows(_run_mysql(fk_sql))

    tables: dict[str, list[tuple[str, str, str]]] = {}
    for parts in col_rows:
        if len(parts) < 4:
            continue
        t, col, ctype, ckey = parts[0], parts[1], parts[2], parts[3]
        if t == "flyway_schema_history":
            continue
        tables.setdefault(t, []).append((col, ctype, ckey))

    edges: list[tuple[str, str, str]] = []
    for parts in fk_rows:
        if len(parts) < 4:
            continue
        ct, cc, pt = parts[0], parts[1], parts[2]
        if ct == "flyway_schema_history" or pt == "flyway_schema_history":
            continue
        edges.append((ct, pt, cc))

    fk_cols = {(child, col) for child, _parent, col in edges}

    lines: list[str] = [
        "# Database ERD",
        "",
        "Entity-relationship view of the application database: foreign keys as relationships, "
        "and key columns (PK, UK, FK) per table. The `flyway_schema_history` table is omitted.",
        "",
        "```mermaid",
        "erDiagram",
    ]

    seen_pairs: set[tuple[str, str, str]] = set()
    for child, parent, col in sorted(edges, key=lambda e: (e[1], e[0], e[2])):
        key = (child, parent, col)
        if key in seen_pairs:
            continue
        seen_pairs.add(key)
        label = col.replace('"', "'")
        lines.append(f"    {_q(parent)} ||--o{{ {_q(child)} : \"{label}\"")

    for t in sorted(tables.keys()):
        cols = tables[t]
        lines.append(f"    {_q(t)} {{")
        for col, ctype, ckey in cols:
            tags: list[str] = []
            if ckey == "PRI":
                tags.append("PK")
            elif ckey == "UNI":
                tags.append("UK")
            if (t, col) in fk_cols:
                tags.append("FK")
            if not tags:
                continue
            mt = _mermaid_type(ctype)
            lines.append(f"        {mt} {col} {' '.join(tags)}")
        lines.append("    }")

    lines.append("```")
    lines.append("")

    OUTPUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT.relative_to(REPO_ROOT)}", file=sys.stderr)


if __name__ == "__main__":
    main()
