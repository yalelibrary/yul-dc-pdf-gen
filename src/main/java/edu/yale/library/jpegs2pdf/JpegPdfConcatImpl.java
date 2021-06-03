package edu.yale.library.jpegs2pdf;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

import edu.yale.library.jpegs2pdf.model.JpegPdfPage;
import edu.yale.library.jpegs2pdf.model.Property;
import edu.yale.library.jpegs2pdf.processor.JpegPdfConcat;



public class JpegPdfConcatImpl implements JpegPdfConcat {

	private List<JpegPdfPage> pages;
	private List<Property> properties;
	private List<Property> addressLines;
	private String header;
	private String documentTitle;
	private PDFont regFont;
	private PDFont boldFont;
	private PDStructureElement currentPart;
	private PDStructureElement currentSection;
	private int mcid = 1;
	private String imageProcessingCommand;
	
	private PDDocument document;
	
	
	public void generatePdf(String header, String documentTitle, List<Property> documentProperties, List<Property> documentAddressLines, List<JpegPdfPage> jpegPdfPages, File destinationFile, String imageProcessingCommand)
			throws IOException {
		this.header = header;
		this.documentTitle = documentTitle;
		this.properties = documentProperties;
		this.addressLines = documentAddressLines;
		this.pages = jpegPdfPages;
		this.imageProcessingCommand = imageProcessingCommand;
		
		long start = System.currentTimeMillis();		
		createDocument();
		loadFonts();	
		addPart();			
		addCoverPageToDocument();			
		addPart();
		addJpegPages();
		document.save(destinationFile);
		document.close();
		
		long time = System.currentTimeMillis() - start;
		System.out.println("Generated: " + pages.size() + " in " + time + (pages.size()>0?(" at " + (time / pages.size())):""));
	}

	private void addJpegPages() throws IOException {
		int ix = 1;
		if ( pages != null ) {
			for (JpegPdfPage page : pages) {
				addJpegPageToDocument(page);
			}
		}
	}

	private void createDocument() {
		MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupTempFileOnly();
		try {
			File tempDir = Files.createTempDirectory("PDFGen").toFile();
			tempDir.deleteOnExit();
			memoryUsageSetting.setTempDir(tempDir);
		} catch ( IOException e ) {
			System.err.println("Error setting temp directory for PDF MemoryUsageSettings");
			e.printStackTrace();
		}
		this.document = new PDDocument(memoryUsageSetting);
		PDDocumentCatalog documentCatalog = this.document.getDocumentCatalog();
		PDStructureTreeRoot structureTreeRoot = new PDStructureTreeRoot();
		document.getDocumentInformation().setCreationDate(Calendar.getInstance());
		documentCatalog.setStructureTreeRoot(structureTreeRoot);
		documentCatalog.setLanguage("English");
		documentCatalog.setViewerPreferences( new PDViewerPreferences(new COSDictionary()));
		documentCatalog.getViewerPreferences().setDisplayDocTitle(true);
		PDMarkInfo markInfo = new PDMarkInfo();
		markInfo.setMarked(true);
		documentCatalog.setMarkInfo(markInfo);

		PDDocumentInformation info = document.getDocumentInformation();
		if (documentTitle != null)
			info.setTitle(documentTitle);
		if (this.properties != null) {
			Property p = findProperty("Author");
			if ( p != null ) {
				info.setAuthor(p.getValue());
			}
			p = findProperty("Subject");
			if ( p != null ) {
				info.setSubject(p.getValue());
			}
		}
	}
	
	private Property findProperty(String title ) {
		for ( Property p : this.properties ) {
			if ( p.getTitle().equals(title)) {
				return p;
			}
		}
		return null;
	}

	private void loadFonts() throws IOException {
		boldFont = PDType0Font.load(document, this.getClass().getResourceAsStream("/FreeSansBold.ttf"));
		regFont = PDType0Font.load(document, this.getClass().getResourceAsStream("/arialunicodems.ttf"));
	}

	private void addCoverPageToDocument() throws IOException {
		PDPage page = new PDPage(PDRectangle.LETTER);
		page.getCOSObject().setItem(COSName.getPDFName("Tabs"), COSName.S);
		document.addPage(page);
		addSection( currentPart);
		PDPageContentStream contentStream = new PDPageContentStream( document, page, AppendMode.OVERWRITE, false);
		// hard code the image and alt text for the image for now.
		BufferedImage bimg = null;
		InputStream in = this.getClass().getResourceAsStream("/logo.png");
		try {
			bimg = ImageIO.read(in);
		} finally {
			in.close();
		}
		int margin = 50;
		float width = bimg.getWidth();
		float height = bimg.getHeight();
		float imageAspect = width / height;
		float x, y, w, h;
		x = margin;
		h = 75;
		y = PDRectangle.LETTER.getHeight() - margin - h;
		w = h * imageAspect;
		PDImageXObject pdImageXObject = LosslessFactory.createFromImage(document, bimg);				
		COSDictionary cosDictionary = beginMarkedConent(contentStream, COSName.IMAGE);
		contentStream.drawImage(pdImageXObject, x, y, w, h);
		contentStream.endMarkedContent();
		addImageToStructure( page, currentSection, pdImageXObject, "Yale University Logo", cosDictionary);
		contentStream.setStrokingColor(new Color(150,150,150));
		contentStream.moveTo(50, PDRectangle.LETTER.getHeight() - 130);
		contentStream.lineTo(PDRectangle.LETTER.getWidth() - 100, PDRectangle.LETTER.getHeight() - 130);
		contentStream.stroke();
		float ypos = drawPropertiesToContentStream(document, page, contentStream, header, properties, 15, 10, 150, new Color(51,90,138), Color.BLUE, StandardStructureTypes.H1);
		ypos += 40;
		
		contentStream.setStrokingColor(new Color(150,150,150));
		contentStream.moveTo(50, PDRectangle.LETTER.getHeight() - ypos + 20 );
		contentStream.lineTo(PDRectangle.LETTER.getWidth() - 100, PDRectangle.LETTER.getHeight() - ypos + 20);
		contentStream.stroke();

		if ( addressLines != null ) {
			drawPropertiesToContentStream(document, page, contentStream, "Contact Information", addressLines, 15, 10, ypos, new Color(51, 90, 138), Color.BLUE, StandardStructureTypes.H2);
		}
		
		
		contentStream.close();
	}

	private float drawWithWidth(PDPageContentStream content, String text, float paragraphWidth, PDFont font,
			int fontSize) throws IOException {
		text = fixText(text);
		int start = 0;
		int end = 0;
		float fontHeight = (font.getFontDescriptor().getCapHeight()) / 1000 * fontSize * (float) 1.5;
		float height = 0;
		for (int i : possibleWrapPoints(text)) {
			float width = font.getStringWidth(text.substring(start, i)) / 1000 * fontSize;
			if (start < end && width > paragraphWidth) {
				content.showText(text.substring(start, end));
				content.newLineAtOffset(0, -fontHeight);
				height += fontHeight;
				start = end;
			}
			end = i;
		}
		content.showText(text.substring(start));
		return height;
	}

	private int[] possibleWrapPoints(String text) {
		String[] split = text.split("(?<=\\W)");
		int[] ret = new int[split.length];
		ret[0] = split[0].length();
		for (int i = 1; i < split.length; i++)
			ret[i] = ret[i - 1] + split[i].length();
		return ret;
	}


	private float drawPropertiesToContentStream(PDDocument document, PDPage page, PDPageContentStream contentStream, 
			String caption,
			List<Property> properties, int titleFontSize, int fontSize, 
			float yPos, 
			Color titleColor, Color linkColor, String headingStructureType )
			throws IOException {
		float margin = 50;
		PDFont labelFont = boldFont;
		PDFont valueFont = regFont;
		float titleFontHeight = (valueFont.getFontDescriptor().getCapHeight()) / 1000 * titleFontSize;
		float fontHeight = (valueFont.getFontDescriptor().getCapHeight()) / 1000 * fontSize;
		COSDictionary dictionary = beginMarkedConent(contentStream, COSName.P);
		contentStream.beginText();
		contentStream.setFont(valueFont, titleFontSize);
		float titleLeading = titleFontHeight * 2;
		float leading = fontHeight * 2;
		contentStream.setLeading(titleLeading);
		contentStream.newLineAtOffset(margin, PDRectangle.LETTER.getHeight() - yPos - fontHeight);
		
		
		if ( titleColor != null ) {
			contentStream.setNonStrokingColor(titleColor);
		}
		
		yPos += drawWithWidth(contentStream, caption, PDRectangle.LETTER.getWidth() - 2 * margin, valueFont, titleFontSize);
		
		if ( titleColor != null ) {
			contentStream.setNonStrokingColor(0,0,0);
		}
		
		

		contentStream.newLine();
		contentStream.endMarkedContent();
		addContentToStructure(  page, currentSection, COSName.P, headingStructureType, dictionary);
		contentStream.setFont(labelFont, fontSize);
		contentStream.setLeading(leading);
		yPos += leading;
		if (properties != null) {
			float maxTitleWidth = 0;
			for (Property property : properties) {
				maxTitleWidth = Math.max(labelFont.getStringWidth(fixText(property.getTitle())) / 1000 * fontSize + 10, maxTitleWidth);
			}
			float paraWidth = PDRectangle.LETTER.getWidth() - 2 * margin - maxTitleWidth;
			for (Property property: properties ) {

				contentStream.setFont(labelFont, fontSize);
				dictionary = beginMarkedConent(contentStream, COSName.P);
				contentStream.showText(fixText(property.getTitle()) + " ");
				contentStream.newLineAtOffset(maxTitleWidth, 0);
				contentStream.setFont(valueFont, fontSize);
				boolean link = linkColor != null && isValueLink(property.getValue());
				if ( link ) {
					contentStream.setNonStrokingColor(linkColor);
				}
				yPos += drawWithWidth(contentStream, fixText(property.getValue()), paraWidth, valueFont, fontSize);
				if ( link ) {
					contentStream.setNonStrokingColor(Color.BLACK);
				}	
				// contentStream.drawString( entry.getValue() );
				contentStream.newLineAtOffset(-maxTitleWidth, -fontHeight * (float) 2.2);
				contentStream.endMarkedContent();
				addContentToStructure(  page, currentSection, COSName.P, StandardStructureTypes.P, dictionary);	
				yPos += fontHeight * (float) 2.2;

			}
		}
		contentStream.endText();
		return yPos;
	}

	public static String fixText(String text ) {
		return text.replace("\u00a0", " ")
				.replace('\n', ' ')
				.replace('\r', ' ')
				.replace('\t', ' ')
				.replaceAll("\\p{Cc}", "")
                                .replace("\u202A", "")
                                .replace("\u200F", "");
	}

	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
			Pattern.CASE_INSENSITIVE);

	public static boolean isValidEmail(String emailStr) {
		Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
		return matcher.find();
	}


	private boolean isValueLink(String value) {
		if ( value.startsWith("http") ) return true;
		if ( isValidEmail(value) ) return true;
		return false;
	}

	private void addJpegPageToDocument(JpegPdfPage jpegPdfPage) throws IOException {
		float margin = 50;
		PDPage page = new PDPage(PDRectangle.LETTER);
		page.getCOSObject().setItem(COSName.getPDFName("Tabs"), COSName.S);
		document.addPage(page);
		PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.OVERWRITE, true);
		addSection( currentPart);
		float yPos = drawPropertiesToContentStream(document, page, contentStream, jpegPdfPage.getCaption(), jpegPdfPage.getProperties(), 12, 9, 50, null, null, StandardStructureTypes.H1);
		yPos -= 40;
		drawImageOnPage(jpegPdfPage, page, contentStream, yPos, margin);
		contentStream.close();
	}

	private void drawImageOnPage(JpegPdfPage jpegPdfPage, PDPage page, PDPageContentStream contentStream, float yPos,
			float margin) throws IOException {
		InputStream in = jpegPdfPage.createInputStream();
		BufferedImage bimg = getBufferedImage(in);
		float width = bimg.getWidth();
		float height = bimg.getHeight();
		float pageAspect = PDRectangle.LETTER.getWidth() / (PDRectangle.LETTER.getHeight() - yPos);
		float imageAspect = width / height;
		float x, y, w, h;
		if (pageAspect > imageAspect) {
			y = margin;
			h = PDRectangle.LETTER.getHeight() - yPos - 2 * margin;
			w = h * imageAspect;
			x = (PDRectangle.LETTER.getWidth() - w) / 2;
		} else {
			x = margin;
			w = PDRectangle.LETTER.getWidth() - 2 * margin;
			h = w / imageAspect;
			y = (PDRectangle.LETTER.getHeight() - yPos - h) / 2;
		}
//		if ( imageAspect > 1 ) {
//			bimg = resizeImage(bimg, 2000, (int)(2000.0 / imageAspect));
//		} else {
//			bimg = resizeImage(bimg, (int)(2000.0 * imageAspect), 2000);
//		}
		PDImageXObject pdImageXObject = JPEGFactory.createFromImage(document, bimg);
		COSDictionary cosDictionary = beginMarkedConent(contentStream, COSName.IMAGE);
		contentStream.drawImage(pdImageXObject, x, y, w, h);
		contentStream.endMarkedContent();
		addImageToStructure( page, currentSection, pdImageXObject, jpegPdfPage.getCaption(), cosDictionary);
	}

	private BufferedImage getBufferedImage(InputStream in) throws IOException {
		BufferedImage bimg;
		File processingInputFile = null;
		File processingOutputFile = null;
		if (imageProcessingCommand != null) {
			processingInputFile = File.createTempFile("pdfPageOriginal", "img");
			java.nio.file.Files.copy(
					in,
					processingInputFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			in.close();
			processingOutputFile = File.createTempFile("pdfPageConverted", ".jpg");
			processingOutputFile.delete();
			int errorCount = 0;
			boolean successful = false;
			while (!successful) {
				try {
					Process process;
					String cmd = String.format(imageProcessingCommand, processingInputFile.getAbsolutePath(), processingOutputFile.getAbsolutePath());
					process = Runtime.getRuntime()
							.exec(cmd);
					StreamGobbler streamGobbler =
							new StreamGobbler(process.getInputStream(), System.err::println);
					Executors.newSingleThreadExecutor().submit(streamGobbler);
					int exitCode = 0;
					try {
						exitCode = process.waitFor();
					} catch (InterruptedException e) {
						throw new IOException(e);
					}
					if (exitCode == 0 && processingOutputFile.exists()) {
						in = new FileInputStream(processingOutputFile);
						successful = true;
					} else {
						throw new IOException("Preprocessing failed: " + cmd);
					}
				} catch (IOException e) {
					errorCount++;
					if (errorCount > 5) throw e;
					else try {Thread.sleep(100);} catch (InterruptedException intErr){}
				}
			}
		}
		try {
			bimg = ImageIO.read(in);
		} finally {
			in.close();
		}
		if (processingInputFile != null) {
			processingInputFile.delete();
		}
		if (processingOutputFile != null) {
			processingOutputFile.delete();
		}
		return bimg;
	}

	private BufferedImage resizeImage( BufferedImage image, int width, int height ) {
		if ( image.getWidth() < width ) {
			width = image.getWidth();
			height = image.getHeight();
		}
		BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = newImage.createGraphics();
		graphics2D.drawImage(image, 0, 0, width, height, null);
		graphics2D.dispose();
		return newImage;
	}
	
	
	private COSDictionary beginMarkedConent(PDPageContentStream contentStream, COSName name ) throws IOException {
		COSDictionary cosDictionary = new COSDictionary();
		cosDictionary.setInt(COSName.MCID, mcid);
		mcid++;
		contentStream.beginMarkedContent(name, PDPropertyList.create(cosDictionary));
		return cosDictionary;
	}
	
	private void addImageToStructure(PDPage page, PDStructureElement part, PDImageXObject pdImageXObject, String altText, COSDictionary cosDictionary) {

		PDStructureElement structureElement = new PDStructureElement(StandardStructureTypes.Figure, part);
		structureElement.setPage(page);
		part.appendKid(structureElement);
		PDMarkedContent markedContent = new PDMarkedContent(COSName.IMAGE, cosDictionary );
		markedContent.addXObject(pdImageXObject);
		structureElement.appendKid(markedContent);
		if ( altText != null ) {
			cosDictionary.setString(COSName.ALT, altText);
			structureElement.setAlternateDescription(altText);			
		}
	}
	
	private PDStructureElement addPart() {

		PDStructureElement part = new PDStructureElement(StandardStructureTypes.PART, document.getDocumentCatalog().getStructureTreeRoot());
		document.getDocumentCatalog().getStructureTreeRoot().appendKid(part);
		currentPart = part;
		return part;
	}
	

	
	private PDStructureElement addSection(PDStructureElement parent) {
		PDStructureElement sect = new PDStructureElement(StandardStructureTypes.SECT, parent);
		parent.appendKid(sect);
		currentSection = sect;
		return sect;
	}
	
 
	private void addContentToStructure(PDPage page, PDStructureElement part, COSName name, String type, COSDictionary cosDictionary) {

		PDStructureElement structureElement = new PDStructureElement(type, part);
		structureElement.setPage(page);
		PDMarkedContent markedContent = new PDMarkedContent(name, cosDictionary);
		structureElement.appendKid(markedContent);
		part.appendKid(structureElement);
		
	}

	private static class StreamGobbler implements Runnable {
		private InputStream inputStream;
		private Consumer<String> consumer;

		public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
			this.inputStream = inputStream;
			this.consumer = consumer;
		}

		@Override
		public void run() {
			new BufferedReader(new InputStreamReader(inputStream)).lines()
					.forEach(consumer);
		}
	}
	

}
