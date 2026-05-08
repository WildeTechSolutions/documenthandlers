module com.thomaswilde.documents.pdf.pdfbox {
    requires org.apache.pdfbox;
    requires java.desktop;
    requires org.slf4j;
    requires com.thomaswilde.form;
    requires commons.beanutils;
    requires preflight;

    exports com.thomaswilde.documents.pdf.pdfbox;
}
