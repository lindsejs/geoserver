package org.geoserver.catalog;

import java.util.List;

import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.config.GeoServerPersister;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

public class CatalogIntegrationTest extends GeoServerTestSupport {
    
    @Override
    protected boolean useLegacyDataDirectory() {
        return false;
    }

    public void testWorkspaceRemoveAndReadd() {
        // remove all workspaces
        Catalog catalog = getCatalog();
        NamespaceInfo defaultNamespace = catalog.getDefaultNamespace();
        WorkspaceInfo defaultWs = catalog.getDefaultWorkspace();
        List<WorkspaceInfo> workspaces = catalog.getWorkspaces();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        for (WorkspaceInfo ws : workspaces) {
            visitor.visit(ws);
        }
        assertEquals(0, catalog.getWorkspaces().size());
        assertEquals(0, catalog.getNamespaces().size());
        
        // add back one (this would NPE)
        catalog.add(defaultNamespace);
        catalog.add(defaultWs);
        assertEquals(1, catalog.getWorkspaces().size());
        assertEquals(1, catalog.getNamespaces().size());
        
        // get back by name (this would NPE too)
        assertNotNull(catalog.getNamespaceByURI(defaultNamespace.getURI()));
    }
    
    /**
     * Checks that the namespace/workspace listener keeps on working after
     * a catalog reload
     */
    public void testNamespaceWorkspaceListenerAttached() throws Exception {
        Catalog catalog = getCatalog();
        
        NamespaceInfo ns = catalog.getNamespaceByPrefix(MockData.CITE_PREFIX);
        String newName = "XYWZ1234";
        ns.setPrefix(newName);
        catalog.save(ns);
        assertNotNull(catalog.getWorkspaceByName(newName));
        assertNotNull(catalog.getNamespaceByPrefix(newName));
        
        // force a reload
        int listenersBefore = catalog.getListeners().size();
        getGeoServer().reload();
        int listenersAfter = catalog.getListeners().size();
        assertEquals(listenersBefore, listenersAfter);
        
        // check the NamespaceWorkspaceListener is still attached and working
        ns = catalog.getNamespaceByPrefix(newName);
        ns.setPrefix(MockData.CITE_PREFIX);
        catalog.save(ns);
        assertNotNull(catalog.getWorkspaceByName(MockData.CITE_PREFIX));
        
        // make sure we only have one resource pool listener and one catalog persister
        int countCleaner = 0;
        int countPersister = 0;
        for (CatalogListener listener : catalog.getListeners()) {
            if(listener instanceof ResourcePool.CacheClearingListener) {
                countCleaner++;
            } else if(listener instanceof GeoServerPersister) {
                countPersister++;
            }
        }
        assertEquals(1, countCleaner);
        assertEquals(1, countPersister);
    }
}
