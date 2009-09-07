/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;

/**
 * A search field definition. For each field in the configuration file
 * searchfields.xml there is one MCRFieldDef instance with attributes that
 * represent the configuration in the xml file.
 * 
 * @author Frank L�tzenkirchen
 */
public class MCRFieldDef {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRFieldDef.class);

    private static Hashtable<String, MCRFieldDef> fieldTable = new Hashtable<String, MCRFieldDef>();

    public final static Namespace xalanns = Namespace.getNamespace("xalan",
            "http://xml.apache.org/xalan");

    public final static Namespace extns = Namespace.getNamespace("ext",
            "xalan://org.mycore.services.fieldquery.MCRData2Fields");

    private final static String configFile = "searchfields.xml";

    /**
     * Read searchfields.xml and build the MCRFiedDef objects
     */
    static {
        Element def = getConfigFile();

        List children = def.getChildren("index", MCRConstants.MCR_NAMESPACE);

        for (int i = 0; i < children.size(); i++) {
            Element index = (Element) (children.get(i));
            String id = index.getAttributeValue("id");

            List fields = index.getChildren("field", MCRConstants.MCR_NAMESPACE);

            for (int j = 0; j < fields.size(); j++)
                new MCRFieldDef(id, (Element) fields.get(j));
        }
    }

    /**
     * The index this field belongs to
     */
    private String index;

    /**
     * The unique name of the field
     */
    private String name;

    /**
     * The data type of the field, see fieldtypes.xml file
     */
    private String dataType;

    /**
     * If true, this field can be used to sort query results
     */
    private boolean sortable = false;

    /**
     * A list of object names this field is used for, separated by blanks
     */
    private String objects;

    /**
     * A keyword identifying where the values of this field come from
     */
    private String source;

    /**
     * If true, the content of this field will be added by searcher as metadata
     * in hit
     */
    private boolean addable = false;

    /**
     * @return the searchfields-configuration file as jdom-element
     */
    public static Element getConfigFile() {
        String uri = "resource:" + configFile;
        return MCRURIResolver.instance().resolve(uri);
    }

    public MCRFieldDef(String index, Element def) {
        this.index = index;
        this.name = def.getAttributeValue("name");
        this.dataType = def.getAttributeValue("type");
        this.sortable = "true".equals(def.getAttributeValue("sortable"));
        this.objects = def.getAttributeValue("objects", (String) null);
        this.source = def.getAttributeValue("source");
        this.addable = "true".equals(def.getAttributeValue("addable"));

        if (!fieldTable.contains(name)) {
            fieldTable.put(name, this);
        } else {
            throw new MCRException("Field \"" + name + "\" is defined repeatedly.");
        }
        buildForEachXSL(def);
    }

    /**
     * Returns the MCRFieldDef with the given name, or throws
     * MCRConfigurationException if no such field is defined in searchfields.xml
     * 
     * @param name
     *            the name of the field
     * @return the MCRFieldDef instance representing this field
     */
    public static MCRFieldDef getDef(String name) {
        if (!fieldTable.containsKey(name)) {
            throw new MCRConfigurationException("Field \"" + name + "\" is not defined in searchfields.xml");
        }
        return fieldTable.get(name);
    }

    /**
     * Returns all fields that belong to the given index
     * 
     * @param index
     *            the ID of the index
     * @return a List of MCRFieldDef objects for that index
     */
    public static List<MCRFieldDef> getFieldDefs(String index) {
        List<MCRFieldDef> fields = new ArrayList<MCRFieldDef>();
        for (MCRFieldDef field : fieldTable.values()) {
            if (field.index.equals(index))
                fields.add(field);
        }
        return fields;
    }

    /**
     * Returns the ID of the index this field belongs to
     * 
     * @return the index ID
     */
    public String getIndex() {
        return index;
    }

    /**
     * Returns the name of the field
     * 
     * @return the field's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the data type of this field as defined in fieldtypes.xml
     * 
     * @return the data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns true if this field can be used as sort criteria
     * 
     * @return true, if field can be used to sort query results
     */
    public boolean isSortable() {
        return sortable;
    }

    /**
     * Returns true if this field should be added by searcher to hit
     * 
     * @return true, if this field should be added by searcher to hit
     */
    public boolean isAddable() {
        return addable;
    }

    /**
     * Returns true if this field is used for this type of object. For
     * MCRObject, the type is the same as in MCRObject.getId().getTypeId(). For
     * MCRFile, the type is the same as in MCRFile.getContentTypeID(). For plain
     * XML data, the type is the name of the root element.
     * 
     * @param objectType
     *            the type of object
     */
    public boolean isUsedForObjectType(String objectType) {
        if (objects == null)
            return true;

        String a = " " + objects + " ";
        String b = " " + objectType + " ";
        return (a.indexOf(b) >= 0);
    }

    /**
     * A keyword identifying that the source of the values of this field is the
     * createXML() method of MCRObject
     * 
     * @see org.mycore.datamodel.metadata.MCRObject#createXML()
     */
    public final static String OBJECT_METADATA = "objectMetadata";

    /**
     * A keyword identifying that the source of the values of this field is a
     * classification category that a MCRObject belongs to
     * 
     * @see org.mycore.datamodel.metadata.MCRObject#createXML()
     */
    public final static String OBJECT_CATEGORY = "objectCategory";

    /**
     * A keyword identifying that the source of the values of this field is the
     * createXML() method of MCRFile
     * 
     * @see org.mycore.datamodel.ifs.MCRFile#createXML()
     */
    public final static String FILE_METADATA = "fileMetadata";

    /**
     * A keyword identifying that the source of the values of this field is the
     * getAllAdditionalData() method of MCRFile
     * 
     * @see org.mycore.datamodel.ifs.MCRFilesystemNode#getAllAdditionalData()
     */
    public final static String FILE_ADDITIONAL_DATA = "fileAdditionalData";

    /**
     * A keyword identifying that the source of the values of this field is the
     * XML content of the MCRFile
     * 
     * @see org.mycore.datamodel.ifs.MCRFile#getContentAsJDOM()
     */
    public final static String FILE_XML_CONTENT = "fileXMLContent";

    /**
     * A keyword identifying that the source of the values of this field is the
     * XML content of a pure JDOM xml document
     */
    public final static String XML = "xml";

    /**
     * A keyword identifying that the source of the values of this field is the
     * text content of the MCRFile, using text filter plug-ins.
     */
    public final static String FILE_TEXT_CONTENT = "fileTextContent";

    /**
     * A keyword identifying that the source of the values of this field is the
     * MCRSearcher that does the search, this means it is technical hit metadata
     * added by the searcher when the query results are built.
     */
    public final static String SEARCHER_HIT_METADATA = "searcherHitMetadata";

    /**
     * Returns a keyword identifying where the values of this field come from.
     * 
     * @see #FILE_METADATA
     * @see #FILE_TEXT_CONTENT
     * @see #FILE_XML_CONTENT
     * @see #OBJECT_METADATA
     * @see #OBJECT_CATEGORY
     * @see #SEARCHER_HIT_METADATA
     * @see #XML
     */
    public String getSource() {
        return source;
    }

    /**
     * The stylesheet fragment to build values for this field from XML source
     * data
     */
    private Element xsl;

    /** Returns a stylesheet to build values for this field from XML source data */
    Element getXSL() {
        if (xsl == null)
            return null;
        else
            return (Element) (xsl.clone());
    }

    /**
     * Builds the stylesheet fragment to build values for this field from XML
     * source data
     */
    private void buildForEachXSL(Element fieldDef) {
        String xpath = fieldDef.getAttributeValue("xpath");
        if ((xpath == null) || (xpath.trim().length() == 0)) {
            return;
        }

        // <xsl:if test="contains(@objects,$objecType)">
        if (objects != null) {
            Element xif = new Element("if", MCRConstants.XSL_NAMESPACE);
            xif.setAttribute("test", "contains('" + objects.trim() + "',$objectType)");
            xsl = xif;
        }

        // <xsl:for-each select="{@xpath}">
        Element forEach1 = new Element("for-each", MCRConstants.XSL_NAMESPACE);
        forEach1.setAttribute("select", xpath);
        if (xsl == null)
            xsl = forEach1;
        else
            xsl.addContent(forEach1);

        Element current = fieldDef;
        while (true) {
            List<Namespace> namespaces = current.getAdditionalNamespaces();
            for (Namespace ns : namespaces)
                xsl.addNamespaceDeclaration(ns);
            if (current.isRootElement())
                break;
            else
                current = current.getParentElement();
        }

        if (MCRFieldDef.OBJECT_CATEGORY.equals(fieldDef.getAttributeValue("source"))) {
            // current(): <format classid="DocPortal_class_00000006"
            // categid="FORMAT0002"/>
            // URI: classification:metadata:levels:parents:{class}:{categ}
            Element forEach2 = new Element("for-each", MCRConstants.XSL_NAMESPACE);
            forEach1.addContent(forEach2);
            String uri = "document(concat('classification:metadata:0:parents:',current()/@classid,':',current()/@categid))//category";
            forEach2.setAttribute("select", uri);
            forEach1 = forEach2;
        }

        // <name>value</name>
        Element fieldValue = new Element(getName());
        forEach1.addContent(fieldValue);

        // <xsl:value-of select="{@value}" />
        String valueExpr = fieldDef.getAttributeValue("value");
        Element valueOf = new Element("value-of", MCRConstants.XSL_NAMESPACE);
        valueOf.setAttribute("select", valueExpr);
        fieldValue.addContent(valueOf);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("---------- XSL for search field \"" + name + "\" ----------");
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            LOGGER.debug("\n" + out.outputString(xsl));
        }
    }
}
