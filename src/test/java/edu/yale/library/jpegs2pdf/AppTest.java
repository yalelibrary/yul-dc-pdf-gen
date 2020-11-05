package edu.yale.library.jpegs2pdf;

import junit.framework.TestCase;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.json.*;
import java.io.*;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RunWith(JUnit4.class)
public class AppTest extends TestCase {
    private File jsonFile;
    private File imagejsonFile;
    private File pdfFile;
    private File imageFile;
    private File imagepdfFile;

    @Test
    public void testApp() throws Exception {
        assertFalse("pdf file exist", pdfFile.exists());
        String[] args = {jsonFile.getAbsolutePath(), pdfFile.getAbsolutePath()};
        App.main( args );
        assertTrue("pdf file exist", pdfFile.exists());
        assertTrue("pdf file has length", pdfFile.length()>500);
        assertTrue("pdf file valid", isPdfValid(pdfFile));
    }

    @Test
    public void testAppWithImage() throws Exception {
        assertFalse("imagepdf file exist", imagepdfFile.exists());
        String[] args = {imagejsonFile.getAbsolutePath(), imagepdfFile.getAbsolutePath()};
        App.main( args );
        assertTrue("imagepdf file exist", imagepdfFile.exists());
        assertTrue("imagepdf file has length", imagepdfFile.length()>500);
        assertTrue("imagepdf file valid", isPdfValid(imagepdfFile));
    }

    private boolean isPdfValid(File pdfFile) {
        try {
            PDDocument.load(pdfFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Before
    public void createFile() throws IOException {
        jsonFile = resourceToTempFile("/fixturepdf.json");
        imageFile = resourceToTempFile("/image_not_found.png");
        pdfFile  = File.createTempFile("test1", ".pdf");
        imagepdfFile  = File.createTempFile("test1", ".pdf");
        imagejsonFile  = File.createTempFile("test1", ".json");
        pdfFile.delete();
        imagepdfFile.delete();
        String json = resourceToString("/imagepdf.json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(imagejsonFile));
        json = json.replace("TESTIMAGE", imageFile.getAbsolutePath());
        writer.append(json);

        writer.close();
    }

    String resourceToString (String resourceName)
            throws IOException {
        return new String(IOUtils.toByteArray(this.getClass().getResourceAsStream(resourceName)));
    }

    public File resourceToTempFile(String resourceName)
            throws IOException {

        InputStream initialStream = this.getClass().getResourceAsStream(resourceName);
        File targetFile = File.createTempFile("test1", ".json");

        java.nio.file.Files.copy(
                initialStream,
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        IOUtils.closeQuietly(initialStream);
        return targetFile;
    }

}


