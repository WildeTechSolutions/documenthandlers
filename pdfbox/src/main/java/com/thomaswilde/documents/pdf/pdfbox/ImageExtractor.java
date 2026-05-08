package com.thomaswilde.documents.pdf.pdfbox;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

public class ImageExtractor extends PDFStreamEngine {
    private int imageNumber = 1;
    private int jpgNumber = 0;
    private int pngNumber = 0;
    private Path directory;
    private String fileNameBase;
    private long bytesWrittenPng = 0;
    private long bytesWrittenJpg = 0;

    ImageExtractor(Path directory, String fileNameBase){
        this.directory = directory;
        this.fileNameBase = fileNameBase;
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if( "Do".equals(operation) )
        {
            COSName objectName = (COSName) operands.get( 0 );
            PDXObject xobject = getResources().getXObject( objectName );
            if( xobject instanceof PDImageXObject)
            {
                PDImageXObject image = (PDImageXObject)xobject;

                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                if(imageWidth > 10 && imageNumber < 11) {
                    System.out.println("suffix: " + image.getSuffix());
//                File outputFile = new File(System.getProperty("user.home") + "\\Downloads\\test\\image_" + imageNumber + "." + image.getSuffix());
                    File outputFile = directory.resolve(fileNameBase + "_" + imageNumber + "." + image.getSuffix()).toFile();
                    System.out.println("outputfile: " + outputFile.getAbsolutePath());

                    if (image.getSuffix().toLowerCase().equals("jpg") || image.getSuffix().toLowerCase().equals("jpeg")) {
                        // same image to local
                        BufferedImage bImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
                        bImage = image.getImage();

                        jpgNumber++;

                        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
                        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        jpegParams.setCompressionQuality(0.1f);


//                ImageIO.write(bImage,image.getSuffix(),outputFile);

                        ImageWriter writer = ImageIO.getImageWritersByFormatName(image.getSuffix()).next();

//                ImageWriteParam param = writer.getDefaultWriteParam();
//                if (param.canWriteCompressed()) {
//                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//                    param.setCompressionQuality(0.0f);
//                }

                        System.out.println("saving image to: " + outputFile.getAbsolutePath());

                        writer.setOutput(new FileImageOutputStream(outputFile));
                        writer.write(null, new IIOImage(bImage, null, null), jpegParams);
                        bytesWrittenJpg += outputFile.length();
//                    System.out.println("document bytes: " + bytesWrittenJpg);
                        writer.dispose();
                    } else if (image.getSuffix().equals("png")) {

                        pngNumber++;
//                    // create a blank, RGB, same width and height, and a white background
//                    BufferedImage newBufferedImage = new BufferedImage(image.getWidth(),
//                            image.getHeight(), BufferedImage.TYPE_INT_RGB);
//                    newBufferedImage.createGraphics().drawImage(image.getImage(), 0, 0, Color.WHITE, null);
//
//                    // write to jpeg file
//                    outputFile = new File(System.getProperty("user.home") + "\\Downloads\\test\\image_" + imageNumber + ".jpg");
//                    ImageIO.write(newBufferedImage, "jpg", outputFile);
//                    JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
//                    jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//                    jpegParams.setCompressionQuality(0.1f);
//
//
////                ImageIO.write(bImage,image.getSuffix(),outputFile);
//
//                    ImageWriter writer = ImageIO.getImageWritersByFormatName(image.getSuffix()).next();
//
////                ImageWriteParam param = writer.getDefaultWriteParam();
////                if (param.canWriteCompressed()) {
////                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
////                    param.setCompressionQuality(0.0f);
////                }
//
//                    writer.setOutput(new FileImageOutputStream(outputFile));
//                    writer.write(null, new IIOImage(newBufferedImage, null, null), jpegParams);
//                    writer.dispose();

                        // png scaling

//                    System.out.println("Image width: " + imageWidth);
                        int desiredWidth = 200;
                        int scaleFactor = 0;
                        if (imageWidth > desiredWidth)
                            scaleFactor = imageWidth / desiredWidth;
                        else
                            scaleFactor = 1;

                        BufferedImage outputImage = new BufferedImage(imageWidth / scaleFactor, imageHeight / scaleFactor, BufferedImage.TYPE_INT_ARGB);

                        // scales the input image to the output image
                        Graphics2D g2d = outputImage.createGraphics();
                        g2d.drawImage(image.getImage(), 0, 0, imageWidth / scaleFactor, imageHeight / scaleFactor, null);
                        g2d.dispose();

                        // writes to output file
                        ImageIO.write(outputImage, image.getSuffix(), outputFile);

                        bytesWrittenPng += outputFile.length();

                    }


//                System.out.println("Image saved.");
                    imageNumber++;
                }

            }
            else if(xobject instanceof PDFormXObject)
            {
                PDFormXObject form = (PDFormXObject)xobject;
                showForm(form);
            }
        }
        else
        {
            super.processOperator( operator, operands);
        }
    }

}
