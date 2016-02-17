package com.cvent.kvstore;

import static com.cvent.kvstore.KVStore.AUDIT_REGION;
import static com.cvent.kvstore.KVStore.DEFAULT_REGION;
import static com.cvent.kvstore.KVStore.HIERARCHY_SEPARATOR;

/**
 * Utility class for constructing keys
 * <p/>
 * Created by sviswanathan on 2/17/16.
 */
public class KeyProvider {

    // The key for a document for a region for a key
    // auth/alpha/server/applicationConnectors
    public static String keyFor(String document, String region, String key) {
        return document + HIERARCHY_SEPARATOR + region + HIERARCHY_SEPARATOR + key + HIERARCHY_SEPARATOR;
    }

    // The key in the default region for a document for a key
    // auth/default/server/applicationConnectors
    public static String defaultRegionKeyFor(String document, String key) {
        return keyFor(document, DEFAULT_REGION, key);
    }

    // The key for the entire hierarchy for a document for a region
    // auth/alpha
    public static String keyForEntireRegion(String document, String region) {
        return document + HIERARCHY_SEPARATOR + region;
    }

    // The audit key for the entire hierarchy for a document for a region
    // audit/auth/alpha
    public static String auditKeyForEntireRegion(String document, String region) {
        return AUDIT_REGION + HIERARCHY_SEPARATOR + document + HIERARCHY_SEPARATOR + region;
    }

    // The audit key for a document for a region for a key. The key returned here has only 1 more level
    // containing revision numbers.
    // audit/auth/alpha/server/applicationConnectors  Revision 0 adds /0
    // audit/auth/alpha/server/applicationConnectors  Revision 1 adds /1
    public static String auditHierarchyFor(String document, String region, String key) {
        return AUDIT_REGION + HIERARCHY_SEPARATOR + document + HIERARCHY_SEPARATOR + region + HIERARCHY_SEPARATOR + key;
    }

    // The audit key for a document for a region for a key for a revision. This always returns a leaf key
    // audit/auth/alpha/server/applicationConnectors/0
    // audit/auth/alpha/server/applicationConnectors/1
    public static String auditKeyForRevision(String document, String region, String key, int revision) {
        return AUDIT_REGION + HIERARCHY_SEPARATOR + document + HIERARCHY_SEPARATOR + region + HIERARCHY_SEPARATOR + key
              + HIERARCHY_SEPARATOR + revision;
    }

    // The portion of a key after the last "/"
    public static String auditRevisionFromAuditKey(String key) {
       return key.substring(key.lastIndexOf(HIERARCHY_SEPARATOR) + 1);
    }

}
