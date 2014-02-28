/*
 * $Revision$ 
 * $Date$
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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TooManyListenersException;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.transformer.TransformerImpl;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRWrappedContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xsl.MCRErrorListener;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.mycore.common.xsl.MCRTraceListener;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Transforms XML content using a static XSL stylesheet.
 * The stylesheet is configured via
 * 
 * MCR.ContentTransformer.{ID}.Stylesheet

 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXSLTransformer extends MCRParameterizedTransformer {

    private static final int INITIAL_BUFFER_SIZE = 32 * 1024;

    private static final MCRURIResolver URI_RESOLVER = MCRURIResolver.instance();

    private static final MCREntityResolver ENTITY_RESOLVER = MCREntityResolver.instance();

    private static Logger LOGGER = Logger.getLogger(MCRXSLTransformer.class);

    private static MCRTraceListener TRACE_LISTENER = new MCRTraceListener();

    private static boolean TRACE_LISTENER_ENABLED = Logger.getLogger(MCRTraceListener.class).isDebugEnabled();

    private static MCRCache<String, MCRXSLTransformer> INSTANCE_CACHE = new MCRCache<String, MCRXSLTransformer>(100,
        "MCRXSLTransformer instance cache");

    private static long CHECK_PERIOD = MCRConfiguration.instance().getLong("MCR.LayoutService.LastModifiedCheckPeriod",
        60000);

    /** The compiled XSL stylesheet */
    protected MCRTemplatesSource[] templateSources;

    protected Templates[] templates;

    protected long[] modified;

    protected long modifiedChecked;

    protected SAXTransformerFactory tFactory;

    public MCRXSLTransformer(String... stylesheets) {
        this();
        setStylesheets(stylesheets);
    }

    public MCRXSLTransformer() {
        super();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        LOGGER.info("Transformerfactory: " + transformerFactory.getClass().getName());
        transformerFactory.setURIResolver(URI_RESOLVER);
        transformerFactory.setErrorListener(MCRErrorListener.getInstance());
        if (transformerFactory.getFeature(SAXSource.FEATURE) && transformerFactory.getFeature(SAXResult.FEATURE)) {
            this.tFactory = (SAXTransformerFactory) transformerFactory;
        } else {
            throw new MCRConfigurationException("Transformer Factory " + transformerFactory.getClass().getName()
                + " does not implement SAXTransformerFactory");
        }
    }

    public static MCRXSLTransformer getInstance(String... stylesheets) {
        String key = stylesheets.length == 1 ? stylesheets[0] : Arrays.toString(stylesheets);
        MCRXSLTransformer instance = INSTANCE_CACHE.get(key);
        if (instance == null) {
            instance = new MCRXSLTransformer(stylesheets);
            INSTANCE_CACHE.put(key, instance);
        }
        return instance;
    }

    @Override
    public void init(String id) {
        super.init(id);
        String property = "MCR.ContentTransformer." + id + ".Stylesheet";
        String[] stylesheets = MCRConfiguration.instance().getString(property).split(",");
        setStylesheets(stylesheets);
    }

    public void setStylesheets(String... stylesheets) {
        this.templateSources = new MCRTemplatesSource[stylesheets.length];
        for (int i = 0; i < stylesheets.length; i++) {
            this.templateSources[i] = new MCRTemplatesSource(stylesheets[i].trim());
        }
        this.modified = new long[templateSources.length];
        this.modifiedChecked = 0;
        this.templates = new Templates[templateSources.length];
    }

    private void checkTemplateUptodate() throws TransformerConfigurationException, SAXException {
        boolean check = System.currentTimeMillis() - modifiedChecked > CHECK_PERIOD;
        if (check) {
            for (int i = 0; i < templateSources.length; i++) {
                long lastModified = templateSources[i].getLastModified();
                if (templates[i] == null || modified[i] < lastModified) {
                    SAXSource source = templateSources[i].getSource();
                    templates[i] = tFactory.newTemplates(source);
                    if (templates[i] == null) {
                        throw new TransformerConfigurationException("XSLT Stylesheet could not be compiled: "
                            + templateSources[i].getURL());
                    }
                    modified[i] = lastModified;
                }
            }
            modifiedChecked = System.currentTimeMillis();
        }
    }

    @Override
    public String getEncoding() throws TransformerException, SAXException {
        return getOutputProperty("encoding", "UTF-8");
    }

    @Override
    public String getMimeType() throws TransformerException, SAXException {
        return getOutputProperty("media-type", "text/xml");
    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        return transform(source, new MCRParameterCollector());
    }

    @Override
    public MCRContent transform(MCRContent source, MCRParameterCollector parameter) throws IOException {
        try {
            LinkedList<TransformerHandler> transformHandlerList = getTransformHandlerList(parameter);
            XMLReader reader = getXMLReader(transformHandlerList);
            TransformerHandler lastTransformerHandler = transformHandlerList.getLast();
            return transform(source, reader, lastTransformerHandler, parameter);
        } catch (TransformerConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void transform(MCRContent source, OutputStream out) throws IOException {
        transform(source, out, new MCRParameterCollector());
    }

    @Override
    public void transform(MCRContent source, OutputStream out, MCRParameterCollector parameter) throws IOException {
        MCRErrorListener el = null;
        try {
            LinkedList<TransformerHandler> transformHandlerList = getTransformHandlerList(parameter);
            XMLReader reader = getXMLReader(transformHandlerList);
            TransformerHandler lastTransformerHandler = transformHandlerList.getLast();
            el = (MCRErrorListener) lastTransformerHandler.getTransformer().getErrorListener();
            StreamResult result = new StreamResult(out);
            lastTransformerHandler.setResult(result);
            reader.parse(source.getInputSource());
        } catch (TransformerConfigurationException e) {
            throw new IOException(e);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (RuntimeException e) {
            if (el != null && e.getCause() == null && el.getExceptionThrown() != null) {
                //typically if a RuntimeException has no cause, we can get the "real cause" from MCRErrorListener, yeah!!!
                throw new IOException(el.getExceptionThrown());
            }
            throw e;
        }
    }

    protected MCRContent transform(MCRContent source, XMLReader reader, TransformerHandler transformerHandler,
        MCRParameterCollector parameter) throws IOException, SAXException {
        return new MCRTransformedContent(source, reader, transformerHandler, getLastModified(), parameter);
    }

    private long getLastModified() {
        long lastModified = -1;
        for (long current : modified) {
            if (current < 0) {
                return -1;
            }
            lastModified = Math.max(lastModified, current);
        }
        return lastModified;
    }

    protected LinkedList<TransformerHandler> getTransformHandlerList(MCRParameterCollector parameterCollector)
        throws TransformerConfigurationException, SAXException {
        checkTemplateUptodate();
        LinkedList<TransformerHandler> xslSteps = new LinkedList<TransformerHandler>();
        //every transformhandler shares the same ErrorListener instance
        MCRErrorListener errorListener = MCRErrorListener.getInstance();
        for (Templates template : templates) {
            TransformerHandler handler = tFactory.newTransformerHandler(template);
            parameterCollector.setParametersTo(handler.getTransformer());
            handler.getTransformer().setErrorListener(errorListener);
            if (TRACE_LISTENER_ENABLED) {
                TransformerImpl transformer = (TransformerImpl) handler.getTransformer();
                TraceManager traceManager = transformer.getTraceManager();
                try {
                    traceManager.addTraceListener(TRACE_LISTENER);
                } catch (TooManyListenersException e) {
                    LOGGER.warn("Could not add MCRTraceListener.", e);
                }
            }
            if (!xslSteps.isEmpty()) {
                Result result = new SAXResult(handler);
                xslSteps.getLast().setResult(result);
            }
            xslSteps.add(handler);
        }
        return xslSteps;
    }

    /**
     * @param transformHandlerList
     * @return
     * @throws SAXException
     */
    protected XMLReader getXMLReader(LinkedList<TransformerHandler> transformHandlerList) throws SAXException {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(ENTITY_RESOLVER);
        reader.setContentHandler(transformHandlerList.getFirst());
        return reader;
    }

    private String getOutputProperty(String propertyName, String defaultValue) throws TransformerException,
        SAXException {
        checkTemplateUptodate();
        Templates lastTemplate = templates[templates.length - 1];
        Properties outputProperties = lastTemplate.getOutputProperties();
        if (outputProperties == null) {
            return defaultValue;
        }
        String value = outputProperties.getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.transformer.MCRContentTransformer#getFileExtension()
     */
    @Override
    public String getFileExtension() throws TransformerException, SAXException {
        String fileExtension = super.fileExtension;
        if (fileExtension != null && !getDefaultExtension().equals(fileExtension)) {
            return fileExtension;
        }
        //until we have a better solution
        if ("text/html".equals(getMimeType())) {
            return "html";
        }
        if ("text/xml".equals(getMimeType())) {
            return "xml";
        }
        return getDefaultExtension();
    }

    private static class MCRTransformedContent extends MCRWrappedContent {
        private MCRContent source;
    
        private XMLReader reader;
    
        private TransformerHandler transformerHandler;
    
        private long lastModified;
    
        private MCRContent transformed;
    
        private String eTag;
    
        public MCRTransformedContent(MCRContent source, XMLReader reader, TransformerHandler transformerHandler,
            long transformerLastModified, MCRParameterCollector parameter) throws IOException {
            this.source = source;
            this.reader = reader;
            this.transformerHandler = transformerHandler;
            this.lastModified = (transformerLastModified >= 0 && source.lastModified() >= 0) ? Math.max(
                transformerLastModified, source.lastModified()) : -1;
            this.eTag = generateETag(source, lastModified, parameter.hashCode());
        }
    
        private String generateETag(MCRContent content, final long lastModified, final int parameterHashCode)
            throws IOException {
            String sourceETag = content.getETag();
            long systemLastModified = MCRConfiguration.instance().getSystemLastModified();
            StringBuilder b = new StringBuilder(sourceETag);
            b.deleteCharAt(b.length() - 1);//removes at end "
            b.append('/');
            byte[] unencodedETag = ByteBuffer.allocate(Long.SIZE / 4).putLong(lastModified ^ parameterHashCode)
                .putLong(systemLastModified ^ parameterHashCode).array();
            b.append(Base64.encodeBase64String(unencodedETag));
            b.append('"');
            return b.toString();
        }
    
        @Override
        public MCRContent getBaseContent() {
            if (transformed == null) {
                MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream(INITIAL_BUFFER_SIZE);
                StreamResult serializer = new StreamResult(baos);
                transformerHandler.setResult(serializer);
                // Parse the source XML, and send the parse events to the
                // TransformerHandler.
                try {
                    LOGGER.info("Start transforming: " + source.getSystemId());
                    reader.parse(source.getInputSource());
                } catch (IOException | SAXException e) {
                    throw new MCRException(e);
                }
                transformed = new MCRByteContent(baos.getBuffer(), 0, baos.size(), lastModified);
            }
            return transformed;
        }
    
        @Override
        public long lastModified() throws IOException {
            return lastModified;
        }
    
        @Override
        public String getETag() throws IOException {
            return eTag;
        }
    
    }
}
