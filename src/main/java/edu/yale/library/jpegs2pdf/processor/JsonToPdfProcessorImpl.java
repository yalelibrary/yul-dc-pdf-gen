package edu.yale.library.jpegs2pdf.processor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import edu.yale.library.jpegs2pdf.model.JpegPdfPage;
import edu.yale.library.jpegs2pdf.model.Property;

public class JsonToPdfProcessorImpl implements PdfProcessor {
	private JsonReader jsonReader;
	private JpegPdfConcat jpegPdfConcat;
	

	public JsonToPdfProcessorImpl(JsonReader jsonReader, JpegPdfConcat jpegPdfConcat) {
		this.jsonReader = jsonReader;
		this.jpegPdfConcat = jpegPdfConcat;
	}



	@Override
	public void generatePdf( String destinationPdfFilepath) throws IOException {
		
		JsonObject document = jsonReader.readObject();
		jsonReader.close();
		
		
		JsonArray pages = document.getJsonArray("pages");
		List<Property> documentProperties = null;
		
		if ( document.containsKey("properties") ) {
			JsonArray properties = document.getJsonArray("properties");
			documentProperties = propertyListFromJsonArray(properties);
		}

		List<Property> documentAddressLines = null;
		if ( document.containsKey("addressLines") ) {
			JsonArray addressLines = document.getJsonArray("addressLines");
			documentAddressLines = propertyListFromJsonArray(addressLines);
		}
		
		String documentTitle = document.getString("title", "No Title");
		String header = document.getString("header", "Yale University Library Digital Collections") ;

		List<JpegPdfPage> jpegPdfPages = new ArrayList<JpegPdfPage>();
		for (JsonValue pageValue : pages) {
			JsonObject page = (JsonObject) pageValue;
			List<Property> pageProperties = propertyListFromJsonArray(page.getJsonArray("properties"));
			String filename = page.getString("file");
			// download files if necessary
			JpegPdfPage jpegPdfPage = new JpegPdfPage();
			jpegPdfPage.setJpegSource(filename);
			jpegPdfPage.setCaption(page.getString("caption"));
			jpegPdfPage.setProperties(pageProperties);
			jpegPdfPages.add(jpegPdfPage);
		}
		jpegPdfConcat.generatePdf(header, documentTitle, documentProperties, documentAddressLines, jpegPdfPages, new File(destinationPdfFilepath));
	}
	

	private static List<Property> propertyListFromJsonArray(JsonArray properties) throws IOException {
		List<Property> ret = new ArrayList<Property>();
		if (properties != null) {
			for (JsonValue propertyValue : properties) {
				JsonObject property = (JsonObject) propertyValue;
				ret.add(new Property(property.getString("name"), property.getString("value","")));
			}
		}
		return ret;
	}

}

