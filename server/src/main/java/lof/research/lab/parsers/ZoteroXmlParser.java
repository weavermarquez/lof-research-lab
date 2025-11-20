package lof.research.lab.parsers;

import lof.research.lab.data.Record;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZoteroXmlParser {

  /**
   * Parse Zotero XML export file and return list of Records.
   *
   * @param filePath Path to XML file
   * @param limit Maximum number of records to parse (for testing)
   * @return List of parsed Record objects
   */
  public static List<Record> parseXmlFile(String filePath, int limit) {
    List<Record> records = new ArrayList<>();

    try {
      File xmlFile = new File(filePath);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(xmlFile);
      doc.getDocumentElement().normalize();

      NodeList recordNodes = doc.getElementsByTagName("record");
      int count = Math.min(limit, recordNodes.getLength());

      for (int i = 0; i < count; i++) {
        Node recordNode = recordNodes.item(i);
        if (recordNode.getNodeType() == Node.ELEMENT_NODE) {
          Element recordElement = (Element) recordNode;
          Record record = parseRecord(recordElement);
          records.add(record);
        }
      }

      System.out.println("Parsed " + records.size() + " records from " + filePath);

    } catch (Exception e) {
      System.err.println("Error parsing XML: " + e.getMessage());
      e.printStackTrace();
    }

    return records;
  }

  /**
   * Parse a single <record> element into a Record object.
   */
  private static Record parseRecord(Element recordElement) {
    List<String> authors = parseAuthors(recordElement);
    String title = parseTitle(recordElement);
    Integer year = parseYear(recordElement);
    String doi = parseDoi(recordElement);
    String url = parseUrl(recordElement);

    return new Record(authors, title, year, doi, url);
  }

  /**
   * Extract authors from contributors/authors/author or contributors/secondary-authors/author
   */
  private static List<String> parseAuthors(Element recordElement) {
    List<String> authors = new ArrayList<>();

    // Try regular authors first
    NodeList authorNodes = recordElement.getElementsByTagName("authors");
    if (authorNodes.getLength() > 0) {
      Element authorsElement = (Element) authorNodes.item(0);
      NodeList authorList = authorsElement.getElementsByTagName("author");
      for (int i = 0; i < authorList.getLength(); i++) {
        authors.add(authorList.item(i).getTextContent().trim());
      }
    }

    // If no authors, try secondary-authors
    if (authors.isEmpty()) {
      NodeList secondaryAuthorNodes = recordElement.getElementsByTagName("secondary-authors");
      if (secondaryAuthorNodes.getLength() > 0) {
        Element secondaryAuthorsElement = (Element) secondaryAuthorNodes.item(0);
        NodeList authorList = secondaryAuthorsElement.getElementsByTagName("author");
        for (int i = 0; i < authorList.getLength(); i++) {
          authors.add(authorList.item(i).getTextContent().trim());
        }
      }
    }

    return authors;
  }

  /**
   * Extract title from titles/title
   */
  private static String parseTitle(Element recordElement) {
    NodeList titleNodes = recordElement.getElementsByTagName("title");
    if (titleNodes.getLength() > 0) {
      return titleNodes.item(0).getTextContent().trim();
    }
    return null;
  }

  /**
   * Extract year from dates/year
   */
  private static Integer parseYear(Element recordElement) {
    NodeList yearNodes = recordElement.getElementsByTagName("year");
    if (yearNodes.getLength() > 0) {
      try {
        String yearText = yearNodes.item(0).getTextContent().trim();
        return Integer.parseInt(yearText);
      } catch (NumberFormatException e) {
        System.err.println("Could not parse year: " + yearNodes.item(0).getTextContent());
      }
    }
    return null;
  }

  /**
   * Extract DOI from electronic-resource-num
   */
  private static String parseDoi(Element recordElement) {
    NodeList doiNodes = recordElement.getElementsByTagName("electronic-resource-num");
    if (doiNodes.getLength() > 0) {
      return doiNodes.item(0).getTextContent().trim();
    }
    return null;
  }

  /**
   * Extract URL from urls/web-urls/url (first one if multiple)
   */
  private static String parseUrl(Element recordElement) {
    NodeList urlNodes = recordElement.getElementsByTagName("url");
    if (urlNodes.getLength() > 0) {
      return urlNodes.item(0).getTextContent().trim();
    }
    return null;
  }
}
