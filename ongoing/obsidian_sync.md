# User stories — Obsidian ↔ Doughnut (CLI)

**Pull:** Full tree from the notebook head, but fetch/write **only what changed** (e.g. compare last-modified or equivalent timestamps on server vs local).

**Attachments:** Not downloaded; they stay in Doughnut.

**Conflicts (v1):** Last-write-wins plus a clear warning when the other side had newer or divergent edits.

---

1. **Doughnut UI:** wiki-style `[[link]]` in note details are **clickable**.
2. **Doughnut UI:** **auto-update** `[[link]]` link text when the target title changes.
3. User runs CLI to **init** a folder as the bound root for a notebook; the path is stored so **`/use notebook`** can show it in the status bar.
4. User can **download** the head note and **download/sync children** per the rules above.
5. User can **upload** head note body (details) from local to Doughnut.
6. User can **push** local edits for notes that have ids in frontmatter (title, details, and related fields as defined by the sync contract).
7. User can **sync note detail** and **title** changes in both directions within that contract.
8. User can run a **dry run** before applying.
9. User can **sync parent** into downloaded markdown (e.g. a `[[parent]]`-style property) and **apply parent changes from Doughnut → local**.
10. User can **sync new notes Doughnut → local** when they appear remotely (within the changed-only pull model).
11. User can **sync deletion in both directions** under one story: local → Doughnut and Doughnut → local, with one explicit rule set (not two separate features).
12. Use wiki-style links in **graph RAG**.

---

## ユーザーストーリー（日本語）— Obsidian ↔ Doughnut（CLI）

**Pull:** **notebook** の **head note** から **full tree** を対象にするが、取得・書き込みは **変更分のみ**（例: **server** と **local** の **last-modified** や同等のタイムスタンプで比較）。

**Attachments:** ダウンロードしない。**Doughnut** 上に残す。

**Conflicts (v1):** **last-write-wins**。他方が新しい／分岐している場合は明確な **warning** を出す。

---

1. **Doughnut UI:** ノート **details** 内の wiki 形式 **`[[link]]`** をクリック可能にする。
2. **Doughnut UI:** **link** 先の **title** が変わったとき **`[[link]]`** の表示テキストを **auto-update** する。
3. **CLI** で **`init`** し、フォルダを **notebook** に紐づけた **root** とする。パスを記憶し、**`/use notebook`** 時に **status bar** に表示できる。
4. **download** で **head note** を取得し、上記ルールに従い **children** の **download**／**sync** ができる。
5. **upload** で **head note** の本文（**details**）を **local** → **Doughnut** に送れる。
6. **frontmatter** に **id** のあるノートについて、**local** の編集を **push** できる（**title**、**details**、および **sync contract** で定めた関連フィールド）。
7. 同一 **sync contract** のもとで、**note detail** と **title** の変更を双方向に **sync** できる。
8. 適用前に **dry run** できる。
9. **download** した **markdown** に **parent** を書き込める（例: **`[[parent]]`** 形式のプロパティ）。**parent** の変更を **Doughnut → local** に反映できる。
10. **remote** に新規ノートが現れたら、（**changed-only** の **pull** モデルの範囲で）**Doughnut → local** に新規ノートを **sync** できる。
11. **deletion** は双方向を **ひとつのストーリー**で扱う：**local** → **Doughnut** と **Doughnut** → **local** を、ひと組の明示ルールで扱う（別機能に分けない）。
12. **graph RAG** で wiki 形式の **link** を使う。
