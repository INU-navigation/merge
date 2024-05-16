package com.wegoup.dijkstra;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class KMLParser {
    public KMLParser() {
    }

    public List<NodeInfo> parseAndFilterKML(InputStream inputStream) {
        List<NodeInfo> filteredNodeList = new ArrayList();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            NodeList placemarkList = doc.getElementsByTagName("ns0:Placemark");

            for(int i = 0; i < placemarkList.getLength(); ++i) {
                Node placemarkNode = placemarkList.item(i);
                if (placemarkNode.getNodeType() == 1) {
                    Element placemarkElement = (Element)placemarkNode;
                    NodeList pointList = placemarkElement.getElementsByTagName("ns0:Point");
                    if (pointList.getLength() > 0) {
                        String coordinates = placemarkElement.getElementsByTagName("ns0:coordinates").item(0).getTextContent();
                        String name = placemarkElement.getElementsByTagName("ns0:name").item(0).getTextContent();
                        filteredNodeList.add(new NodeInfo(name, coordinates));
                    }
                }
            }
        } catch (Exception var13) {
            var13.printStackTrace();
        }

        return filteredNodeList;
    }

    public List<String> parseKML(InputStream inputStream) {
        List<String> coordinatesList = new ArrayList();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, (String)null);

            for(int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 2 && parser.getName().equals("coordinates")) {
                    eventType = parser.next();
                    coordinatesList.add(parser.getText());
                }
            }

            inputStream.close();
        } catch (IOException | XmlPullParserException var6) {
            var6.printStackTrace();
        }

        return coordinatesList;
    }

    public static class NodeInfo {
        private String name;
        private String coordinates;

        public NodeInfo(String name, String coordinates) {
            this.name = name;
            this.coordinates = coordinates;
        }

        public String getName() {
            return this.name;
        }

        public String getCoordinates() {
            return this.coordinates;
        }
    }
}

