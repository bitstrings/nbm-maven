package org.bitstrings.maven.nbm.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.Closer;

public class JarPack200
{
    public static final String DEFAULT_GZIPPED_PACKED_FILE_SUFFIX = "pack.gz";
    public static final String DEFAULT_PACKED_FILE_SUFFIX = "pack";

    public static final class DefaultProperties
    {
        public static final String SEGMENT_LIMIT = "-1";

        private DefaultProperties() {}

        public static Packer populatePacker( Packer packer )
        {
            final Map<String, String> packerProps =  packer.properties();

            packerProps.put( Packer.SEGMENT_LIMIT, SEGMENT_LIMIT );

            return packer;
        }
    }

    private final Packer packer;
    private final Unpacker unpacker;

    private boolean useGZip = true;

    public JarPack200()
    {
        this.packer = getDefaultPacker();
        this.unpacker = getDefaultUnpacker();
    }

    public JarPack200( int effort )
    {
        this.packer = getDefaultPacker( effort );
        this.unpacker = getDefaultUnpacker();
    }

    public JarPack200( Packer packer, Unpacker unpacker )
    {
        this.packer = packer;
        this.unpacker = unpacker;
    }

    public void pack( File sourceJarFile, File targetFile )
        throws IOException
    {
        pack( packer, sourceJarFile, targetFile, useGZip );
    }

    protected void pack( File sourceJarFile, File targetFile, boolean useGZip )
        throws IOException
    {
        pack( packer, sourceJarFile, targetFile, useGZip );
    }

    protected void pack( Packer packer, File sourceJarFile, File targetFile, boolean useGZip )
        throws IOException
    {
        final Closer closer = Closer.create();

        try
        {
            final JarFile jarFile = new JarFile( sourceJarFile );

            final OutputStream targetOut =
                        closer.register(
                                useGZip
                                    ? new GZIPOutputStream( new FileOutputStream( targetFile ), 4096 )
                                    : new BufferedOutputStream( new FileOutputStream( targetFile ), 4096 ) );

            packer.pack( jarFile, targetOut );
        }
        finally
        {
            closer.close();
        }
    }

    public void unpack( File sourceFile, File targetJarFile )
        throws IOException
    {
        unpack( unpacker, sourceFile, targetJarFile, useGZip );
    }

    protected void unpack( File sourceFile, File targetJarFile, boolean isGZip )
        throws IOException
    {
        unpack( unpacker, sourceFile, targetJarFile, isGZip );
    }

    protected void unpack( Unpacker unpacker, File sourceFile, File targetJarFile, boolean isGZip )
        throws IOException
    {
        final Closer closer = Closer.create();

        try
        {
            final InputStream jarIn =
                closer.register(
                        isGZip
                            ? new GZIPInputStream( new FileInputStream( sourceFile ), 4096 )
                            : new BufferedInputStream( new FileInputStream( sourceFile ), 4096 ) );

            final JarOutputStream jarOut =
                        closer.register( new JarOutputStream( new FileOutputStream( targetJarFile ) ) );

            unpacker.unpack( jarIn, jarOut );
        }
        finally
        {
            closer.close();
        }
    }

    public void repack( File sourceJarFile )
        throws IOException
    {
        repack( sourceJarFile, sourceJarFile );
    }

    public void repack( File sourceJarFile, File targetJarFile )
        throws IOException
    {
        if ( targetJarFile == null)
        {
            targetJarFile = sourceJarFile;
        }

        final File tempFile = File.createTempFile( "pack200", ".repack." + DEFAULT_GZIPPED_PACKED_FILE_SUFFIX );

        tempFile.deleteOnExit();

        try
        {
            pack( sourceJarFile, tempFile, false );
            unpack( tempFile, targetJarFile, false );
        }
        finally
        {
            tempFile.delete();
        }
    }

    public File getPackedFileFromJarFile( String jarFile )
    {
        return
                new File(
                        jarFile.concat(
                            "." +
                                ( useGZip
                                    ? DEFAULT_GZIPPED_PACKED_FILE_SUFFIX
                                    : DEFAULT_PACKED_FILE_SUFFIX ) ) );
    }

    public File getPackedFileFromJarFile( File jarFile )
    {
        return getPackedFileFromJarFile( jarFile.getAbsolutePath() );
    }

    public static Packer getDefaultPacker()
    {
        return getDefaultPacker( -1 );
    }

    public static Packer getDefaultPacker( int effort )
    {
        final Packer packer = DefaultProperties.populatePacker( Pack200.newPacker() );

        if ( effort > -1 )
        {
            packer.properties().put( Packer.EFFORT, String.valueOf( effort ) );
        }

        return packer;
    }

    public static Unpacker getDefaultUnpacker()
    {
        return Pack200.newUnpacker();
    }
}
