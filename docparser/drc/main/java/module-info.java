module com.thomaswilde.documents.parser {
	requires com.thomaswilde.documents.ms.poi;
//    requires com.thomaswilde.documents.pdf.itext;
    requires com.thomaswilde.documents.pdf.pdfbox;
    requires org.slf4j;
    requires org.apache.commons.io;
    requires java.desktop;

    exports com.thomaswilde.documents.parser;
}
