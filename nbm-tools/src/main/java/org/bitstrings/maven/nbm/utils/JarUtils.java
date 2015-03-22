package org.bitstrings.maven.nbm.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import com.google.common.io.Closer;

// +p
public final class JarUtils
{
    public static final String MANIFEST_JAR_ENTRY = "META-INF/MANIFEST.MF";

    public static final String MANIFEST_ATTR_PERMISSIONS = "Permissions";

    public static final String MANIFEST_ATTR_CODEBASE = "Codebase";

    public static final String MANIFEST_ATTR_APPLICATION_NAME = "Application-Name";

    public static final String MANIFEST_ATTR_TRUSTED_ONLY = "Trusted-Only";

    public static final String MANIFEST_ATTR_TRUSTED_LIBRARY = "Trusted-Library";

    public static final String MANIFEST_ATTR_CLASS_PATH = "Class-Path";

    public static final String
        MANIFEST_ATTR_APPLICATION_LIBRARY_ALLOWABLE_CODEBASE = "Application-Library-Allowable-Codebase";

    public static final String
        MANIFEST_ATTR_CALLER_ALLOWABLE_CODEBASE = "Caller-Allowable-Codebase";

    private JarUtils() {}

    public static void unsignArchive( final File jarFile )
        throws IOException
    {
        archiveModifier( jarFile, null, null, true, null );
    }

    /**
     * Removes any existing signatures from the specified JAR file. We will stream from the input JAR directly to the
     * output JAR to retain as much metadata from the original JAR as possible.
     *
     * @param inJar The JAR file to unsign, must not be <code>null</code>.
     * @throws unsignArchive If the unsigning failed.
     */
    public static void archiveModifier(
            final File inJar, final File outJar,
            Integer compressionLevel,
            boolean unsign,
            Attributes attributes )
        throws IOException
    {
        File workJar =
                    outJar == null
                            ? new File( inJar.getAbsolutePath() + ".tmp" )
                            : outJar;

        Closer closer = Closer.create();
        try
        {
            final JarInputStream jis =
                closer.register( new JarInputStream( new BufferedInputStream( new FileInputStream( inJar ) ) ) );

            final Manifest manifest = getManifest( inJar );

            // 1- remove any existing digest
            // remove all manifest entries (problem when validating non-files entries)
            if ( unsign )
            {
                final Map<String, Attributes> entries = manifest.getEntries();

                entries.clear();

                /*
                for ( Iterator<Map.Entry<String, Attributes>> entryIter = entries.entrySet().iterator() ;
                        entryIter.hasNext(); )
                {
                    final Map.Entry<String, Attributes> entry = entryIter.next();

                    for ( Iterator<Object> attribIter = entry.getValue().keySet().iterator();
                            attribIter.hasNext(); )
                    {
                        final String attribute = attribIter.next().toString();

                        if ( attribute.endsWith( "-Digest" ) )
                        {
                            attribIter.remove();
                        }
                    }

                    if ( entry.getValue().size() == 0 )
                    {
                        entryIter.remove();
                    }
                }
                */
            }

            if ( ( attributes != null ) && !attributes.isEmpty() )
            {
                for ( Map.Entry<Object, Object> attrEntry : attributes.entrySet() )
                {
                    if ( attrEntry.getValue() == null )
                    {
                        manifest.getMainAttributes().remove( attrEntry.getKey() );
                    }
                    else
                    {
                        manifest.getMainAttributes().put( attrEntry.getKey(), attrEntry.getValue() );
                    }
                }
            }

            final JarOutputStream jos =
                        closer.register( new JarOutputStream( new FileOutputStream( workJar ), manifest ) );

            if ( compressionLevel != null )
            {
                jos.setLevel( compressionLevel );
            }

            for ( JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry() )
            {
                if ( unsign && isSignatureFile( je.getName() ) )
                {
                    continue;
                }

                // do not write the MANIFEST entry if we have already set one
                if ( !je.getName().equals( MANIFEST_JAR_ENTRY ) )
                {
                    jos.putNextEntry( new JarEntry( je.getName() ) );

                    IOUtil.copy( jis, jos );
                }
            }
        }
        finally
        {
            closer.close();
        }

        if ( outJar == null )
        {
            FileUtils.rename( workJar, inJar );
        }
    }

    /**
     * Checks whether the specified JAR file entry denotes a signature-related file, i.e. matches
     * <code>META-INF/*.SF</code>, <code>META-INF/*.DSA</code> or <code>META-INF/*.RSA</code>.
     *
     * @param entryName The name of the JAR file entry to check, must not be <code>null</code>.
     * @return <code>true</code> if the entry is related to a signature, <code>false</code> otherwise.
     */
    private static boolean isSignatureFile( String entryName )
    {
        if ( entryName.regionMatches( true, 0, "META-INF", 0, 8 ) )
        {
            entryName = entryName.replace('\\', '/');

            if ( ( entryName.indexOf('/') == 8 ) && ( entryName.lastIndexOf('/') == 8 ) )
            {
                if ( entryName.regionMatches( true, entryName.length() - 3, ".SF", 0, 3 ) )
                {
                    return true;
                }
                if ( entryName.regionMatches( true, entryName.length() - 4, ".DSA", 0, 4 ) )
                {
                    return true;
                }
                if ( entryName.regionMatches( true, entryName.length() - 4, ".RSA", 0, 4 ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    // +p
    // Not using JarInputStream because it is flawed, might not find Manifest
    // if MANIFEST.MF is not the first or second entry which is dumb.
    // This will find it if it exist or create one otherwise.
    public static Manifest getManifest( File file )
        throws IOException
    {
        Closer closer = Closer.create();
        try
        {
            final JarInputStream jis =
                    closer.register(
                        new JarInputStream( new BufferedInputStream( new FileInputStream( file ) ) ) );

            final Manifest manifest = jis.getManifest();

            if ( manifest != null )
            {
                return new Manifest( manifest );
            }

            for ( JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry() )
            {
                if ( je.getName().equals( MANIFEST_JAR_ENTRY ) )
                {
                    return new Manifest( jis );
                }
            }
        }
        finally
        {
            closer.close();
        }

        return new Manifest();
    }

    public static void main(String[] args)
        throws Exception
    {
        System.out.println( new URI( "file:as" ).getScheme() );
    }
}
