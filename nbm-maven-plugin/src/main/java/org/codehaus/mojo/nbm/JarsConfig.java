package org.codehaus.mojo.nbm;

import java.util.List;
import java.util.Map;

import org.apache.maven.model.PatternSet;

/**
 *
 * @author p
 *
 * @since 3.11.1
 */
// +p
public class JarsConfig
{
    public static class ManifestEntries
    {
        private Boolean trustedOnly;

        private Boolean trustedLibrary;

        private String permissions;

        private String codebase;

        private Map<String, String> extraAttributes;

        private List<String> removeAttributes;

        public Boolean getTrustedOnly()
        {
            return trustedOnly;
        }

        public Boolean getTrustedLibrary()
        {
            return trustedLibrary;
        }

        public String getPermissions()
        {
            return permissions;
        }

        public String getCodebase()
        {
            return codebase;
        }

        public Map<String, String> getExtraAttributes()
        {
            return extraAttributes;
        }

        public List<String> getRemoveAttributes()
        {
            return removeAttributes;
        }
    }

    private ManifestEntries manifestEntries;

    private PatternSet jarSet;

    private Boolean removeExistingSignatures;

    public ManifestEntries getManifestEntries()
    {
        return manifestEntries;
    }

    public PatternSet getJarSet()
    {
        return jarSet;
    }

    public Boolean getRemoveExistingSignatures()
    {
        return removeExistingSignatures;
    }
}
