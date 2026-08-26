package com.odde.donut.services.notebookExport;

public record ExportFolderRow(
    Integer id, Integer parentFolderId, String name, String readmeContent) {}
