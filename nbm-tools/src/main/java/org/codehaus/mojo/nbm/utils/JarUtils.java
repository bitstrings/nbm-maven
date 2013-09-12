package org.codehaus.mojo.nbm.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

public final class JarUtils
{
    private JarUtils() {}

    public static void unsignArchive( final File jarFile )
        throws IOException
    {
        unsignArchive( jarFile, null );
    }

    /**
     * Removes any existing signatures from the specified JAR file. We will stream from the input JAR directly to the
     * output JAR to retain as much metadata from the original JAR as possible.
     *
     * @param jarFile The JAR file to unsign, must not be <code>null</code>.
     * @throws unsignArchive If the unsigning failed.
     */
    public static void unsignArchive( final File jarFile, final File to )
        throws IOException
    {
        File unsignedFile =
                    to == null
                            ? new File( jarFile.getAbsolutePath() + ".unsigned" )
                            : to;

        ZipInputStream zis = null;
        ZipOutputStream zos = null;

        try
        {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(jarFile)));
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(unsignedFile)));

            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry())
            {
                if (isSignatureFile(ze.getName()))
                {
                    continue;
                }

                zos.putNextEntry(ze);

                IOUtil.copy(zis, zos);
            }

        }
        finally
        {
            IOUtil.close(zis);
            IOUtil.close(zos);
        }

        if (to == null)
        {
            FileUtils.rename(unsignedFile, jarFile);
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
        if (entryName.regionMatches(true, 0, "META-INF", 0, 8))
        {
            entryName = entryName.replace('\\', '/');

            if (entryName.indexOf('/') == 8 && entryName.lastIndexOf('/') == 8)
            {
                if (entryName.regionMatches(true, entryName.length() - 3, ".SF", 0, 3))
                {
                    return true;
                }
                if (entryName.regionMatches(true, entryName.length() - 4, ".DSA", 0, 4))
                {
                    return true;
                }
                if (entryName.regionMatches(true, entryName.length() - 4, ".RSA", 0, 4))
                {
                    return true;
                }
            }
        }

        return false;
    }
}
