package edu.yale.library.jpegs2pdf;

import java.io.FileReader;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonReader;

import edu.yale.library.jpegs2pdf.processor.JpegPdfConcat;
import edu.yale.library.jpegs2pdf.processor.JsonToPdfProcessorImpl;
import edu.yale.library.jpegs2pdf.processor.PdfProcessor;

public class App {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			throw new IOException("You must provide the json file and output file as arguments.");
		}
		
		String pdfContentFilepath = args[0];
		String destinationFilepath = args[1];
		
		JpegPdfConcat jpegPdfConcat = new JpegPdfConcatImpl();
		JsonReader jsonReader = Json.createReader(new FileReader(pdfContentFilepath));
		
		PdfProcessor pdfProcessor = new JsonToPdfProcessorImpl(jsonReader, jpegPdfConcat);
		pdfProcessor.generatePdf(destinationFilepath);
		
		
	}
}
