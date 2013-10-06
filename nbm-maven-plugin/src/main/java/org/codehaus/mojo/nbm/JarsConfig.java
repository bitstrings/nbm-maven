package org.codehaus.mojo.nbm;

import java.util.Map;

import org.apache.maven.model.PatternSet;

// +p
public class JarsConfig
{
    private PatternSet jarSet;

    private Boolean removeExistingSignatures;

    private String permissions;

    private String codebase;

    private Map<String, String> extraManifestAttributes;

    public PatternSet getJarSet()
    {
        return jarSet;
    }

    public Boolean getRemoveExistingSignatures()
    {
        return removeExistingSignatures;
    }

    public String getPermissions()
    {
        return permissions;
    }

    public String getCodebase()
    {
        return codebase;
    }

    public Map<String, String> getExtraManifestAttributes()
    {
        return extraManifestAttributes;
    }
}
