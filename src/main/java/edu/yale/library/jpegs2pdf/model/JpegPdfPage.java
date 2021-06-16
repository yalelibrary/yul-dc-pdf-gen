package edu.yale.library.jpegs2pdf.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class JpegPdfPage {

	private String jpegSource;
	private String caption;
	private List<Property> properties;

	public InputStream createInputStream() throws IOException {
		String filename = getJpegSource();
		if (filename.startsWith("http://") || filename.startsWith("https://")) {
			return createHttpInputStream(filename, 5);
		} else {
			return new FileInputStream(getJpegSource());
		}
	}

	public String getJpegSource() {
		return jpegSource;
	}

	public void setJpegSource(String jpegSource) {
		this.jpegSource = jpegSource;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	private InputStream createHttpInputStream(String address, int retryCount) throws IOException {
		InputStream in = null;
		int errorCount = 0;
		while (in == null) {
			try {
				URL website = new URL(address);
				in = website.openStream();
			} catch (IOException e) { // IOException is thrown for 500 error.
				errorCount++;
				if (errorCount > retryCount) throw e;  // breaks out after errorCount reaches limit.
				else try {Thread.sleep(1000);} catch (InterruptedException intErr) {}
			}
		}
		return in;
	}

}
