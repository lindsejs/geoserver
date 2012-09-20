package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Test;
import net.opengis.cat.csw20.DescribeRecordType;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.csw.kvp.DescribeRecordKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSWConfiguration;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DescribeRecordTest extends CSWTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeRecordTest());
    }

    public void testKVPReaderNS() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put("namespace",
                "xmlns(csw=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
        raw.put("typename", "csw:Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);

        assertDescribeRecordValid(dr);
    }

    private void assertDescribeRecordValid(DescribeRecordType dr) {
        assertEquals("CSW", dr.getService());
        assertEquals("2.0.2", dr.getVersion());
        assertEquals(2, dr.getTypeName().size());
        assertEquals(new QName("http://www.opengis.net/cat/csw/2.0.2", "Record"), dr.getTypeName()
                .get(0));
        assertEquals(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"),
                dr.getTypeName().get(1));
    }

    public void testKVPReaderNoNamespace() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put("typename", "csw:Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);

        assertDescribeRecordValid(dr);
    }

    public void testKVPReaderDefaultNamespace() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put("namespace",
                "xmlns(=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
        raw.put("typename", "Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);

        assertDescribeRecordValid(dr);
    }

    public void testXMLReader() throws Exception {
        CSWXmlReader reader = new CSWXmlReader("DescribeRecord", "2.0.2", new CSWConfiguration());
        DescribeRecordType dr = (DescribeRecordType) reader.read(null,
                getResourceAsReader("DescribeRecord.xml"), (Map) null);
        assertDescribeRecordValid(dr);
    }
    
    // this is one of the CITE tests, unknown type names should just be ignored
    public void testDummyRecord() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=csw:DummyRecord");
        checkValidationErrors(dom);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(/csw:DescribeRecordResponse)", dom);
        assertXpathEvaluatesTo("0", "count(//csw:SchemaComponent)", dom);
    }

    public void testBasicGetLocalSchema() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord");
        checkValidationErrors(dom);
        // print(dom);

        assertCswRecordSchema(dom, false);
        
        // check we can really read those schemas 
        MockHttpServletResponse response = getAsServletResponse("/schemas/csw/2.0.2/rec-dcterms.xsd");
        assertEquals(200, response.getStatusCode());
        dom = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes("UTF-8")));
        assertXpathEvaluatesTo("dc:SimpleLiteral", "//xs:element[@name='abstract']/@type", dom);
    }
    
    public void testBasicGetCanonicalSchema() throws Exception {
        try {
            CSWInfo csw = getGeoServer().getService(CSWInfo.class);
            csw.setCanonicalSchemaLocation(true);
            getGeoServer().save(csw);
            
            Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord");
            checkValidationErrors(dom);
            // print(dom);

            assertCswRecordSchema(dom, true);
        } finally {
            CSWInfo csw = getGeoServer().getService(CSWInfo.class);
            csw.setCanonicalSchemaLocation(false);
            getGeoServer().save(csw);
        }
    }
    
    public void testBasicPost() throws Exception {
        String request = IOUtils.toString(getResourceAsReader("DescribeCswRecord.xml"));
        Document dom = postAsDOM("csw", request);
        checkValidationErrors(dom);
        // print(dom);

        assertCswRecordSchema(dom, false);
    }

    private void assertCswRecordSchema(Document dom, boolean canonicalSchema) throws XpathException {
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'BriefRecord'])", dom);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'SummaryRecord'])", dom);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'Record'])", dom);
        String root = canonicalSchema ? "http://schemas.opengis.net" : "http://localhost:8080/geoserver/schemas";
        assertXpathEvaluatesTo(
                root + "/csw/2.0.2/rec-dcterms.xsd",
                "//xsd:import[@namespace = 'http://purl.org/dc/terms/']/@schemaLocation", dom);
    }
    
    public void testAlternativeNamespacePrefix() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=fuffa:Record&namespace=xmlns(fuffa=http://www.opengis.net/cat/csw/2.0.2)");
        assertCswRecordSchema(dom, false);
    }
    
    public void testDefaultNamespacePrefix() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=Record&namespace=xmlns(=http://www.opengis.net/cat/csw/2.0.2)");
        // print(dom);
        assertCswRecordSchema(dom, false);
    }
    
    public void testMissingOutputFormat() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord&outputFormat=text/sgml");
        checkOws10Exception(dom, ServiceException.INVALID_PARAMETER_VALUE, "outputFormat");
    }
    
    public void testInvalidSchemaLanguage() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord&schemaLanguage=http://purl.oclc.org/dsdl/schematron");
        checkOws10Exception(dom, ServiceException.INVALID_PARAMETER_VALUE, "schemaLanguage");
    }
    
}
