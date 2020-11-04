package edu.yale.library.jpegs2pdf.processor;

import java.io.IOException;

public interface PdfProcessor {
	
	public void generatePdf( String destinationPdfFilepath) throws IOException;
	

}
