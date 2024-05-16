package com.wegoup.dijkstra;

import android.util.Log;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LineStringParser {
    public LineStringParser() {
    }

    public List<String> parseLineStringCoordinates(InputStream inputStream) {
        List<String> coordinatesList = new ArrayList();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();
            NodeList lineStringList = document.getElementsByTagName("ns0:LineString");

            for(int i = 0; i < lineStringList.getLength(); ++i) {
                Element lineStringElement = (Element)lineStringList.item(i);
                NodeList coordinatesNodeList = lineStringElement.getElementsByTagName("ns0:coordinates");

                for(int j = 0; j < coordinatesNodeList.getLength(); ++j) {
                    Element coordinatesElement = (Element)coordinatesNodeList.item(j);
                    String coordinates = coordinatesElement.getTextContent();
                    coordinatesList.add(coordinates.trim());
                }
            }
        } catch (Exception var13) {
            Log.e("LineStringParserError", "Error parsing XML", var13);
        }

        return coordinatesList;
    }
}
