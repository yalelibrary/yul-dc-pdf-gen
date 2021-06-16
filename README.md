# jpegs2pdf
Java / PDFBox based PDF generator: Builds PDF with one image per page and optional captions, properties, and a cover page.

## Using the App
This application reads a JSON file to build the PDF file.  The JSON contains URLs to images for use in the PDF.

To create a PDF:
- generate the JSON,  
- run the application passing the JSON file and destination pdf file as two arguments.  

See sample json in `/sample-json/` for examples of the json structure.

This java app will download all images to the working directory and generate the PDF. 
After the PDF is generated the application will delete the image files.  If the application succeeds, it will have
an exit code of 0.
 
These image URLs can point to a IIIF server.  Images are adjusted to fit on a single page.
Based on some testing, using IIIF requests with `/full/!700,1000/0/default.jpg` seems to work well.

### Checkout code
```
git clone git@github.com:yalelibrary/jpegs2pdf.git
cd jpegs2pdf
```

### Prerequisites
You will need to have Java and Maven installed to build the project.

See: https://www.baeldung.com/install-maven-on-windows-linux-mac or `brew install maven`

### Building Executable Jar

```
mvn clean compile assembly:single
```

### Generating PDFs
```
java -jar target/jpegs2pdf-1.1.jar <JSON file> <Destination PDF File>
java -jar target/jpegs2pdf-1.1.jar sample-json/2001489.json 2001489.pdf
java -jar target/jpegs2pdf-1.1.jar sample-json/2001937.json 2001937.pdf
```
Open the PDF files in a PDF Viewer.

### Versioning
When the generator needs to be updated, create a new version of the jar.

We are using simple version numbers in the format: `v<major>.<minor>` (e.g. `v1.1`).  Typically, if a new version is going to be released, increase the 
minor version and create a new release.

First, update the pom.xml file so that the `<version>1.1</version>` tag is updated to match the release, 
with the "v" removed. Create a PR and get this change into the `production` branch.
  
Create the release so that it is based on `production` with the updated pom file.

Make the tag and release name the version: e.g. v1.2, v1.3, etc.

Building the jar with `mvn clean compile assembly:single` to create the new jar in `target/` with the new version in 
the file name.

Don't forget to update the name of the jar in any invocation code if necessary.