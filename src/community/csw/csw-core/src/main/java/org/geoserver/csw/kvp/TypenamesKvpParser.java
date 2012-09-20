package org.geoserver.csw.kvp;

import org.geoserver.ows.KvpParser;

/**
 * We should not need this, but unfortunately the WFS typeNames parser can hijack the parsing
 * and return a list of strings matched against the catalog, which is something we don't want
 * for CSW
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class TypenamesKvpParser extends KvpParser {

    public TypenamesKvpParser() {
        super("typenames", String.class);
        setService("csw");
    }

    @Override
    public Object parse(String value) throws Exception {
        return value;
    }

}
