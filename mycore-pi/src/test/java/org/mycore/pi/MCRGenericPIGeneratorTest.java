package org.mycore.pi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import static org.junit.Assert.assertEquals;

public class MCRGenericPIGeneratorTest extends MCRStoreTestCase {

    public static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    @Test
    public void testGenerate() throws MCRPersistentIdentifierException {
        final MCRGenericPIGenerator generator = new MCRGenericPIGenerator("test1",
            "urn:nbn:de:gbv:$CurrentDate-$0-$1-$ObjectType-$ObjectProject-$ObjectNumber-$Count-",
            new SimpleDateFormat("yyyy", Locale.ROOT), null, null, 3,
            "dnbUrn", "/mycoreobject/metadata/test1/test2/text()", "/mycoreobject/metadata/test1/test3/text()");

        MCRObjectID testID = MCRObjectID.getInstance("my_test_00000001");
        MCRObject mcrObject = new MCRObject();
        mcrObject.setSchema("test");
        mcrObject.setId(testID);
        final Element metadata = new Element("metadata");
        final Element testElement = new Element("test1");
        metadata.addContent(testElement);
        testElement.setAttribute("class", "MCRMetaXML");
        testElement.addContent(new Element("test2").setText("result1"));
        testElement.addContent(new Element("test3").setText("result2"));
        mcrObject.getMetadata().setFromDOM(metadata);

        final String pi1 = generator.generate(mcrObject, "").asString();
        final String pi2 = generator.generate(mcrObject, "").asString();
        assertEquals("urn:nbn:de:gbv:" + CURRENT_YEAR + "-result1-result2-test-my-00000001-000-", pi1.substring(0, pi1.length() - 1));
        assertEquals("urn:nbn:de:gbv:" + CURRENT_YEAR + "-result1-result2-test-my-00000001-001-", pi2.substring(0, pi2.length() - 1));
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());

        return testProperties;
    }
}
