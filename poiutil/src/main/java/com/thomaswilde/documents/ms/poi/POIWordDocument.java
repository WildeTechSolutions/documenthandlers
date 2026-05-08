package com.thomaswilde.documents.ms.poi;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class POIWordDocument {

    private XWPFDocument document;
    private POIXMLProperties.CoreProperties meta;

    public POIWordDocument(String path) throws IOException {

        Path filePath = Paths.get(path);

        if (Files.exists(filePath)) {
            try(InputStream fis = new FileInputStream(filePath.toFile())){
                document = new XWPFDocument(fis);
                meta = document.getProperties().getCoreProperties();
            }
        }
    }

    public void replaceText(String placeholder, String replacementText){
        POIUtil.replacePOI(document, placeholder, replacementText);
    }



    public POIWordDocument(InputStream is) throws IOException {
        initDocument(is);
        is.close();
    }

    private void initDocument(InputStream is) throws IOException {
        document = new XWPFDocument(is);
        meta = document.getProperties().getCoreProperties();
    }

    public void setCreator(String creator){
        meta.setCreator(creator);
    }

    public void setTitle(String title){
        meta.setTitle(title);
    }

    public void setSubject(String subject){
        meta.setSubjectProperty(subject);
    }

    /**
     *
     * @param keyWords Semi-colon separated list of key words
     */
    public void setKeyWords(String keyWords){
        meta.setKeywords(keyWords);
    }

    public void save(Path destination){
        POIUtil.saveDocument(destination, document);
    }

    public void close() throws IOException {
        document.close();
    }

}
