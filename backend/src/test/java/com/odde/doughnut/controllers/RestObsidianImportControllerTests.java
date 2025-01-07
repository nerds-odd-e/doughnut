package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class RestObsidianImportControllerTests {
    private RestObsidianImportController controller;
    private MakeMe makeMe;
    private UserModel userModel;
    private Note parentNote;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestObsidianImportController(modelFactoryService, obsidianImportService);
        parentNote = makeMe.aNote().creatorAndOwner(userModel).please();
    }

    @Test
    void shouldImportObsidianNotesUnderParentNote() throws UnexpectedNoAccessRightException, IOException {
        // Create a mock zip file with Obsidian notes
        MultipartFile zipFile = createMockZipFile("Note 2.md", "# Note 2\nSome content");

        // Import the zip file under the parent note
        NoteRealm importedNote = controller.importObsidianNotes(zipFile, parentNote.getId());

        // Verify the imported note
        assertThat(importedNote.getNote().getTopicConstructor(), equalTo("Note 2"));
        assertThat(importedNote.getNote().getParent().getId(), equalTo(parentNote.getId()));
    }

    @Test
    void shouldThrowExceptionWhenUserHasNoAccessToParentNote() {
        // Create a note owned by a different user
        Note otherUsersNote = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
        MultipartFile zipFile = createMockZipFile("Note 2.md", "# Note 2\nSome content");

        // Attempt to import under a note the user doesn't have access to
        assertThrows(UnexpectedNoAccessRightException.class, () -> 
            controller.importObsidianNotes(zipFile, otherUsersNote.getId())
        );
    }

    private MultipartFile createMockZipFile(String filename, String content) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(filename);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new MockMultipartFile(
            "file",
            "notes.zip",
            "application/zip",
            baos.toByteArray()
        );
    }
} 