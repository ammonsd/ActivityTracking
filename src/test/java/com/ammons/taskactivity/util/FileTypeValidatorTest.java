package com.ammons.taskactivity.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileTypeValidator.
 * 
 * Tests Issue #10 (Magic Number Validation): - Valid JPEG/PNG/PDF files accepted - Files with
 * mismatched magic numbers rejected - Fake files (executables, scripts) rejected - Corrupted files
 * rejected
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
class FileTypeValidatorTest {

    private final FileTypeValidator validator = new FileTypeValidator();

    // Magic number signatures
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] EXE_SIGNATURE = {0x4D, 0x5A}; // MZ (DOS/Windows executable)

    @Test
    void testValidJpegAccepted() throws IOException {
        // Create valid JPEG file
        byte[] jpegContent = createFileWithSignature(JPEG_SIGNATURE, 100);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/jpeg");

        assertTrue(result.isSuccess(), "Valid JPEG should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    void testValidPngAccepted() throws IOException {
        // Create valid PNG file
        byte[] pngContent = createFileWithSignature(PNG_SIGNATURE, 100);
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/png");

        assertTrue(result.isSuccess(), "Valid PNG should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    void testValidPdfAccepted() throws IOException {
        // Create valid PDF file
        byte[] pdfContent = createFileWithSignature(PDF_SIGNATURE, 100);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent);

        FileTypeValidator.ValidationResult result =
                validator.validateFileType(file, "application/pdf");

        assertTrue(result.isSuccess(), "Valid PDF should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    void testImageJpgNormalizedToJpeg() throws IOException {
        // Create valid JPEG file but use image/jpg content type
        byte[] jpegContent = createFileWithSignature(JPEG_SIGNATURE, 100);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpg", jpegContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/jpg");

        assertTrue(result.isSuccess(), "image/jpg should be normalized to image/jpeg");
    }

    @Test
    void testExecutableRejectedWithFakeJpegContentType() throws IOException {
        // Create file with executable signature but fake JPEG content type
        byte[] exeContent = createFileWithSignature(EXE_SIGNATURE, 100);
        MockMultipartFile file = new MockMultipartFile("file", "malware.jpg", // Fake extension
                "image/jpeg", // Fake content type
                exeContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/jpeg");

        assertFalse(result.isSuccess(), "Executable with fake JPEG type should be rejected");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("does not match declared type"),
                "Error should mention type mismatch");
    }

    @Test
    void testTextFileRejectedWithFakePngContentType() throws IOException {
        // Create text file pretending to be PNG
        byte[] textContent = "This is not an image".getBytes();
        MockMultipartFile file =
                new MockMultipartFile("file", "fake.png", "image/png", textContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/png");

        assertFalse(result.isSuccess(), "Text file with fake PNG type should be rejected");
        assertTrue(result.getErrorMessage().contains("does not match declared type"));
    }

    @Test
    void testScriptRejectedWithFakePdfContentType() throws IOException {
        // Create JavaScript file pretending to be PDF
        byte[] scriptContent = "console.log('malicious')".getBytes();
        MockMultipartFile file =
                new MockMultipartFile("file", "script.pdf", "application/pdf", scriptContent);

        FileTypeValidator.ValidationResult result =
                validator.validateFileType(file, "application/pdf");

        assertFalse(result.isSuccess(), "Script with fake PDF type should be rejected");
    }

    @Test
    void testFileTooSmallRejected() throws IOException {
        // Create file smaller than PNG signature (8 bytes)
        byte[] tinyContent = {0x01, 0x02};
        MockMultipartFile file =
                new MockMultipartFile("file", "tiny.png", "image/png", tinyContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/png");

        assertFalse(result.isSuccess(), "File too small should be rejected");
        assertTrue(
                result.getErrorMessage().contains("too small")
                        || result.getErrorMessage().contains("corrupted"),
                "Error should mention file size or corruption");
    }

    @Test
    void testEmptyFileRejected() {
        MockMultipartFile file =
                new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/jpeg");

        assertFalse(result.isSuccess(), "Empty file should be rejected");
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void testNullFileRejected() {
        FileTypeValidator.ValidationResult result = validator.validateFileType(null, "image/jpeg");

        assertFalse(result.isSuccess(), "Null file should be rejected");
        assertTrue(result.getErrorMessage().contains("empty or null"));
    }

    @Test
    void testMissingContentTypeRejected() throws IOException {
        byte[] jpegContent = createFileWithSignature(JPEG_SIGNATURE, 100);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, null);

        assertFalse(result.isSuccess(), "Missing content type should be rejected");
        assertTrue(result.getErrorMessage().contains("Content-Type"));
    }

    @Test
    void testUnsupportedContentTypeRejected() throws IOException {
        byte[] content = "Some content".getBytes();
        MockMultipartFile file =
                new MockMultipartFile("file", "test.svg", "image/svg+xml", content);

        FileTypeValidator.ValidationResult result =
                validator.validateFileType(file, "image/svg+xml");

        assertFalse(result.isSuccess(), "Unsupported content type should be rejected");
        assertTrue(result.getErrorMessage().contains("Unsupported file type"));
    }

    @Test
    void testJpegWithPartialMatch() throws IOException {
        // Create file that starts with FF D8 but not followed by FF
        byte[] partialJpeg = new byte[100];
        partialJpeg[0] = (byte) 0xFF;
        partialJpeg[1] = (byte) 0xD8;
        partialJpeg[2] = (byte) 0x00; // Wrong third byte

        MockMultipartFile file =
                new MockMultipartFile("file", "partial.jpg", "image/jpeg", partialJpeg);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/jpeg");

        assertFalse(result.isSuccess(), "Partial JPEG signature match should be rejected");
    }

    @Test
    void testPdfWithWrongSignature() throws IOException {
        // Create file claiming to be PDF but starting with wrong bytes
        byte[] fakePdf = new byte[100];
        fakePdf[0] = 0x50; // P
        fakePdf[1] = 0x44; // D
        fakePdf[2] = 0x46; // F
        fakePdf[3] = 0x00; // Wrong - should be %PDF

        MockMultipartFile file =
                new MockMultipartFile("file", "fake.pdf", "application/pdf", fakePdf);

        FileTypeValidator.ValidationResult result =
                validator.validateFileType(file, "application/pdf");

        assertFalse(result.isSuccess(), "PDF with wrong signature should be rejected");
    }

    @Test
    void testContentTypeWithCharset() throws IOException {
        // Test content type with charset parameter
        byte[] jpegContent = createFileWithSignature(JPEG_SIGNATURE, 100);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg; charset=UTF-8", jpegContent);

        FileTypeValidator.ValidationResult result =
                validator.validateFileType(file, "image/jpeg; charset=UTF-8");

        assertTrue(result.isSuccess(), "Content type with charset should be handled correctly");
    }

    @Test
    void testLargeValidJpeg() throws IOException {
        // Test with larger file (5MB)
        byte[] jpegContent = createFileWithSignature(JPEG_SIGNATURE, 5 * 1024 * 1024);
        MockMultipartFile file =
                new MockMultipartFile("file", "large.jpg", "image/jpeg", jpegContent);

        FileTypeValidator.ValidationResult result = validator.validateFileType(file, "image/jpeg");

        assertTrue(result.isSuccess(), "Large valid JPEG should be accepted");
    }

    /**
     * Helper method to create file content with specified signature and total size.
     */
    private byte[] createFileWithSignature(byte[] signature, int totalSize) {
        byte[] content = new byte[totalSize];
        System.arraycopy(signature, 0, content, 0, signature.length);
        // Fill rest with dummy data
        for (int i = signature.length; i < totalSize; i++) {
            content[i] = (byte) (i % 256);
        }
        return content;
    }
}
