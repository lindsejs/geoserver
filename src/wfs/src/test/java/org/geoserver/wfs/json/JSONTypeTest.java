/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class JSONTypeTest extends TestCase {

    
    public void testMimeType() {
        
        //MimeType
        assertNotSame(JSONType.json, JSONType.jsonp);
        assertTrue(JSONType.isJsonMimeType(JSONType.json));
        assertTrue(JSONType.isJsonpMimeType(JSONType.jsonp));
        
    }
    
    public void testJSONType() {
        // ENUM type
        JSONType json=JSONType.JSON;
        assertEquals(JSONType.JSON,json);
        JSONType jsonp=JSONType.JSONP;
        assertEquals(JSONType.JSONP,jsonp);
        
        assertEquals(JSONType.JSON,JSONType.getJSONType(JSONType.json));
        assertEquals(JSONType.JSONP,JSONType.getJSONType(JSONType.jsonp));
    }

    public void testCallbackFunction() {
        Map<String, Map<String, String>> kvp=new HashMap<String, Map<String, String>>();
        
        assertEquals(JSONType.CALLBACK_FUNCTION,JSONType.getCallbackFunction(kvp));
        
        Map<String, String> formatOpts = new HashMap<String, String>();
        kvp.put("FORMAT_OPTIONS", formatOpts);
        
        assertEquals(JSONType.CALLBACK_FUNCTION,JSONType.getCallbackFunction(kvp));
        
        formatOpts.put(JSONType.CALLBACK_FUNCTION_KEY, "functionName");
        
        assertEquals("functionName",JSONType.getCallbackFunction(kvp));
    }

}