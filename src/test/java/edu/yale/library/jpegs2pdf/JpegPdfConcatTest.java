package edu.yale.library.jpegs2pdf;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;

/**
 * Unit test for simple App.
 */
public class JpegPdfConcatTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public JpegPdfConcatTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( JpegPdfConcatTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    public void testFontFound() throws IOException {
        String text = "شمس المعارف ول◌ٔطائف العوارف،";
        String textWithBox = "شمس المعارف ول◌□طائف العوارف،";
        PDDocument document = new PDDocument();
        PDFont arabicRegFont = PDType0Font.load(document, this.getClass().getResourceAsStream("/NotoNaskhArabic-Regular.ttf"));
        PDFont latinRegFont = PDType0Font.load(document, this.getClass().getResourceAsStream("/arialunicodems.ttf"));
        JpegPdfConcatImpl jpegPdfConcat = new JpegPdfConcatImpl();

        JpegPdfConcatImpl.FontAndText fontAndText = jpegPdfConcat.pickFontAndText(text, new PDFont[] {latinRegFont, arabicRegFont});
        assertTrue("Text is not changed if font is available", fontAndText.getText().equals(text));

        fontAndText = jpegPdfConcat.pickFontAndText(text, new PDFont[] {latinRegFont});
        assertTrue("Text is changed if font is not found", !fontAndText.getText().equals(text));
        assertTrue("Text has the box for replacement", fontAndText.getText().equals(textWithBox));
    }
}
