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

        private String applicationName;

        private String applicationLibraryAllowableCodebase;

        private String callerAllowableCodebase;

        private Map<String, String> extraAttributes;

        private List<String> removeAttributes;

        public Boolean getTrustedOnly()
        {
            return trustedOnly;
        }

        public void setTrustedOnly(Boolean trustedOnly)
        {
            this.trustedOnly = trustedOnly;
        }

        public Boolean getTrustedLibrary()
        {
            return trustedLibrary;
        }

        public void setTrustedLibrary(Boolean trustedLibrary)
        {
            this.trustedLibrary = trustedLibrary;
        }

        public String getPermissions()
        {
            return permissions;
        }

        public void setPermissions(String permissions)
        {
            this.permissions = permissions;
        }

        public String getCodebase()
        {
            return codebase;
        }

        public void setCodebase(String codebase)
        {
            this.codebase = codebase;
        }

        public String getApplicationName()
        {
            return applicationName;
        }

        public void setApplicationName(String applicationName)
        {
            this.applicationName = applicationName;
        }

        public String getApplicationLibraryAllowableCodebase()
        {
            return applicationLibraryAllowableCodebase;
        }

        public void setApplicationLibraryAllowableCodebase(String applicationLibraryAllowableCodebase)
        {
            this.applicationLibraryAllowableCodebase = applicationLibraryAllowableCodebase;
        }

        public String getCallerAllowableCodebase()
        {
            return callerAllowableCodebase;
        }

        public void setCallerAllowableCodebase(String callerAllowableCodebase)
        {
            this.callerAllowableCodebase = callerAllowableCodebase;
        }

        public Map<String, String> getExtraAttributes()
        {
            return extraAttributes;
        }

        public void setExtraAttributes(Map<String, String> extraAttributes)
        {
            this.extraAttributes = extraAttributes;
        }

        public List<String> getRemoveAttributes()
        {
            return removeAttributes;
        }

        public void setRemoveAttributes(List<String> removeAttributes)
        {
            this.removeAttributes = removeAttributes;
        }
    }

    private ManifestEntries manifestEntries;

    private PatternSet jarSet;

    private Boolean removeExistingSignatures;

    public ManifestEntries getManifestEntries()
    {
        return manifestEntries;
    }

    public void setManifestEntries(ManifestEntries manifestEntries)
    {
        this.manifestEntries = manifestEntries;
    }

    public PatternSet getJarSet()
    {
        return jarSet;
    }

    public void setJarSet(PatternSet jarSet)
    {
        this.jarSet = jarSet;
    }

    public Boolean getRemoveExistingSignatures()
    {
        return removeExistingSignatures;
    }

    public void setRemoveExistingSignatures(Boolean removeExistingSignatures)
    {
        this.removeExistingSignatures = removeExistingSignatures;
    }
}
