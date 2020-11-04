package edu.yale.library.jpegs2pdf.processor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import edu.yale.library.jpegs2pdf.model.JpegPdfPage;
import edu.yale.library.jpegs2pdf.model.Property;

public interface JpegPdfConcat {
	void generatePdf(String header, String documentTitle,  List<Property> documentProperties, List<Property>  documentAddressLines, List<JpegPdfPage> jpegPdfPages, File destinationFile) throws IOException;
		
}
