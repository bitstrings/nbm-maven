/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import static com.google.common.base.Objects.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.IsSigned;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.selectors.And;
import org.apache.tools.ant.types.resources.selectors.Name;
import org.apache.tools.ant.types.resources.selectors.Not;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.ResourceUtils;
import org.bitstrings.maven.nbm.utils.JarPack200;
import org.bitstrings.maven.nbm.utils.JarUtils;

/**
 * Signs JAR or ZIP files with the javasign command line tool. The tool detailed
 * dependency checking: files are only signed if they are not signed. The
 * <tt>signjar</tt> attribute can point to the file to generate; if this file
 * exists then its modification date is used as a cue as to whether to resign
 * any JAR file.
 *
 * Timestamp driven signing is based on the unstable and inadequately documented
 * information in the Java1.5 docs
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/time-of-signing-beta1.html">
 * beta documentation</a>
 * @ant.task category="java"
 * @since Ant 1.1
 */
public class SignJar extends AbstractJarSignerTask {
    // CheckStyle:VisibilityModifier OFF - bc

    // +p -->
    public static interface SigningListener
    {
        void beforeSigning( JarConfigResolved jarConfig );
    }

    private SigningListener jarListener;

    public void setSigningListener(SigningListener jarListener)
    {
        this.jarListener = jarListener;
    }

    public SigningListener getJarListener()
    {
        return jarListener;
    }

    public static class JarConfigResolved
    {
        private boolean unsignFirst;

        private Attributes extraManifestAttributes;

        public JarConfigResolved( boolean unsignFirst, Attributes extraManifestAttributes )
        {
            this.unsignFirst = unsignFirst;
            this.extraManifestAttributes = extraManifestAttributes;
        }

        public boolean isUnsignFirst()
        {
            return unsignFirst;
        }

        public Attributes getExtraManifestAttributes()
        {
            return extraManifestAttributes;
        }
    }

    public static class JarsConfig
    {
        private String includes;

        private String excludes;

        private Boolean unsignFirst;

        private List<Property> extraManifestAttributes;

        private List<String> removeAttributes;


        public String getIncludes()
        {
            return includes;
        }

        public void setIncludes(String includes)
        {
            this.includes = includes;
        }

        public String getExcludes()
        {
            return excludes;
        }

        public Boolean getUnsignFirst()
        {
            return unsignFirst;
        }

        public void setUnsignFirst(Boolean unsignFirst)
        {
            this.unsignFirst = unsignFirst;
        }

        public void setExcludes(String excludes)
        {
            this.excludes = excludes;
        }

        public List<Property> getExtraManifestAttributes()
        {
            return extraManifestAttributes;
        }

        public void setExtraManifestAttributes(List<Property> extraManifestAttributes)
        {
            this.extraManifestAttributes = extraManifestAttributes;
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
    // <-- +p

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * name to a signature file
     */
    protected String sigfile;

    /**
     * name of a single jar
     */
    protected File signedjar;

    /**
     * flag for internal sf signing
     */
    protected boolean internalsf;

    /**
     * sign sections only?
     */
    protected boolean sectionsonly;

    /**
     * flag to preserve timestamp on modified files
     */
    private boolean preserveLastModified;

    /**
     * Whether to assume a jar which has an appropriate .SF file in is already
     * signed.
     */
    protected boolean lazy;

    /**
     * the output directory when using paths.
     */
    protected File destDir;

    /**
     * mapper for todir work
     */
    private FileNameMapper mapper;

    /**
     * URL for a tsa; null implies no tsa support
     */
    protected String tsaurl;

    /**
     * alias for the TSA in the keystore
     */
    protected String tsacert;

    /**
     * force signing even if the jar is already signed.
     */
    private boolean force = false;

    /**
     * signature algorithm
     */
    private String sigAlg;

    /**
     * digest algorithm
     */
    private String digestAlg;

    // +p
    private int retryCount = 1;

    // +p
    private boolean unsignFirst;

    // +p
    private List<Property> extraManifestAttributes;

    // +p
    private List<JarsConfig> jarsConfigs;

    // +p
    private File basedir;

    // +p
    private boolean destFlatten;

    // +p
    private boolean pack200 = false;

    // +p
    private Integer pack200Effort;

    // +p
    private JarPack200 jarPack200;

    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_TODIR_AND_SIGNEDJAR
            = "'destdir' and 'signedjar' cannot both be set";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_TOO_MANY_MAPPERS = "Too many mappers";
    /**
     * error string for unit test verification {@value}
     */
    public static final String ERROR_SIGNEDJAR_AND_PATHS
        = "You cannot specify the signed JAR when using paths or filesets";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_BAD_MAP = "Cannot map source file to anything sensible: ";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_MAPPER_WITHOUT_DEST
        = "The destDir attribute is required if a mapper is set";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_NO_ALIAS = "alias attribute must be set";
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_NO_STOREPASS = "storepass attribute must be set";
    // CheckStyle:VisibilityModifier ON

    public void setRetryCount( int retryCount )
    {
        this.retryCount = retryCount;
    }

    /**
     * name of .SF/.DSA file; optional
     *
     * @param sigfile the name of the .SF/.DSA file
     */
    public void setSigfile(final String sigfile) {
        this.sigfile = sigfile;
    }

    /**
     * name of signed JAR file; optional
     *
     * @param signedjar the name of the signed jar file
     */
    public void setSignedjar(final File signedjar) {
        this.signedjar = signedjar;
    }

    /**
     * Flag to include the .SF file inside the signature; optional; default
     * false
     *
     * @param internalsf if true include the .SF file inside the signature
     */
    public void setInternalsf(final boolean internalsf) {
        this.internalsf = internalsf;
    }

    /**
     * flag to compute hash of entire manifest; optional, default false
     *
     * @param sectionsonly flag to compute hash of entire manifest
     */
    public void setSectionsonly(final boolean sectionsonly) {
        this.sectionsonly = sectionsonly;
    }

    /**
     * flag to control whether the presence of a signature file means a JAR is
     * signed; optional, default false
     *
     * @param lazy flag to control whether the presence of a signature
     */
    public void setLazy(final boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * Optionally sets the output directory to be used.
     *
     * @param destDir the directory in which to place signed jars
     * @since Ant 1.7
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }


    /**
     * add a mapper to determine file naming policy. Only used with toDir
     * processing.
     *
     * @param newMapper the mapper to add.
     * @since Ant 1.7
     */
    public void add(FileNameMapper newMapper) {
        if (mapper != null) {
            throw new BuildException(ERROR_TOO_MANY_MAPPERS);
        }
        mapper = newMapper;
    }

    /**
     * get the active mapper; may be null
     * @return mapper or null
     * @since Ant 1.7
     */
    public FileNameMapper getMapper() {
        return mapper;
    }

    /**
     * get the -tsaurl url
     * @return url or null
     * @since Ant 1.7
     */
    public String getTsaurl() {
        return tsaurl;
    }

    /**
     *
     * @param tsaurl the tsa url.
     * @since Ant 1.7
     */
    public void setTsaurl(String tsaurl) {
        this.tsaurl = tsaurl;
    }

    /**
     * get the -tsacert option
     * @since Ant 1.7
     * @return a certificate alias or null
     */
    public String getTsacert() {
        return tsacert;
    }

    /**
     * set the alias in the keystore of the TSA to use;
     * @param tsacert the cert alias.
     */
    public void setTsacert(String tsacert) {
        this.tsacert = tsacert;
    }

    /**
     * Whether to force signing of a jar even it is already signed.
     * @since Ant 1.8.0
     */
    public void setForce(boolean b) {
        force = b;
    }

    /**
     * Should the task force signing of a jar even it is already
     * signed?
     * @since Ant 1.8.0
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Signature Algorithm; optional
     *
     * @param sigAlg the signature algorithm
     */
    public void setSigAlg(String sigAlg) {
        this.sigAlg = sigAlg;
    }

    /**
     * Signature Algorithm; optional
     */
    public String getSigAlg() {
        return sigAlg;
    }

    /**
     * Digest Algorithm; optional
     *
     * @param digestAlg the digest algorithm
     */
    public void setDigestAlg(String digestAlg) {
        this.digestAlg = digestAlg;
    }

    /**
     * Digest Algorithm; optional
     */
    public String getDigestAlg() {
        return digestAlg;
    }

    public boolean isUnsignFirst()
    {
        return unsignFirst;
    }

    public void setUnsignFirst(boolean unsignFirst)
    {
        this.unsignFirst = unsignFirst;
    }

    public List<Property> getExtraManifestAttributes()
    {
        return extraManifestAttributes;
    }

    public void setExtraManifestAttributes(List<Property> extraManifestAttributes)
    {
        this.extraManifestAttributes = extraManifestAttributes;
    }

    public List<JarsConfig> getJarsConfigs()
    {
        return jarsConfigs;
    }

    public void setJarsConfigs(List<JarsConfig> jarsConfigs)
    {
        this.jarsConfigs = jarsConfigs;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setBasedir(File basedir)
    {
        this.basedir = basedir;
    }

    public boolean isDestFlatten()
    {
        return destFlatten;
    }

    public void setDestFlatten(boolean destFlatten)
    {
        this.destFlatten = destFlatten;
    }

    public boolean isPack200()
    {
        return pack200;
    }

    public void setPack200(boolean pack200)
    {
        this.pack200 = pack200;
    }

    public Integer getPack200Effort()
    {
        return pack200Effort;
    }

    public void setPack200Effort(Integer pack200Effort)
    {
        this.pack200Effort = pack200Effort;
    }

    /**
     * sign the jar(s)
     *
     * @throws BuildException on errors
     */
    @Override
    public void execute() throws BuildException {
        //validation logic
        final boolean hasJar = jar != null;
        final boolean hasSignedJar = signedjar != null;
        final boolean hasDestDir = destDir != null;
        final boolean hasMapper = mapper != null;

        if (!hasJar && !hasResources()) {
            throw new BuildException(ERROR_NO_SOURCE);
        }
        if (null == alias) {
            throw new BuildException(ERROR_NO_ALIAS);
        }

        if (null == storepass) {
            throw new BuildException(ERROR_NO_STOREPASS);
        }

        if (hasDestDir && hasSignedJar) {
            throw new BuildException(ERROR_TODIR_AND_SIGNEDJAR);
        }


        if (hasResources() && hasSignedJar) {
            throw new BuildException(ERROR_SIGNEDJAR_AND_PATHS);
        }

        //this isnt strictly needed, but by being fussy now,
        //we can change implementation details later
        if (!hasDestDir && hasMapper) {
            throw new BuildException(ERROR_MAPPER_WITHOUT_DEST);
        }

        if ( pack200 )
        {
            jarPack200 =
                pack200Effort == null
                        ? new JarPack200()
                        : new JarPack200( pack200Effort );
        }

        beginExecution();

        try
        {
            final Attributes globalExtraManifestAttributes = buildManifestAttributes( extraManifestAttributes );

            final JarConfigResolved defaultJarsConfigInternal =
                        new JarConfigResolved( unsignFirst, globalExtraManifestAttributes );

            //special case single jar handling with signedjar attribute set
            if (hasJar && hasSignedJar) {

                FileResource source = new FileResource( getProject(), jar );

                signOneJar(
                        jar, signedjar,
                        firstNonNull(
                            buildJarsConfigInternalMap( source, globalExtraManifestAttributes ).get( source.getName() ),
                            defaultJarsConfigInternal ) );

                return;
            }

            //the rest of the method treats single jar like
            //a nested path with one file
            Path sources = createUnifiedSourcePath();

            //set up our mapping policy
            FileNameMapper destMapper;
            if (hasMapper) {
                destMapper = mapper;
            } else {
                //no mapper? use the identity policy
                destMapper = new IdentityMapper();
            }


            // +p -->
            Map<String, JarConfigResolved> jarsConfigsMap =
                            buildJarsConfigInternalMap( sources, globalExtraManifestAttributes );

            //at this point the paths are set up with lists of files,
            //and the mapper is ready to map from source dirs to dest files
            //now we iterate through every JAR giving source and dest names
            // deal with the paths
            Iterator<Resource> iter = sources.iterator();
            while (iter.hasNext()) {
                Resource r = iter.next();

                FileResource fr = ResourceUtils
                    .asFileResource( r.as(FileProvider.class) );

                if ( getBasedir() != null )
                {
                    fr.setBaseDir( getBasedir() );
                }

                //calculate our destination directory; it is either the destDir
                //attribute, or the base dir of the fileset (for in situ updates)
                File toDir = hasDestDir ? destDir : fr.getBaseDir();

                //determine the destination filename via the mapper
                String[] destFilenames = destMapper.mapFileName(fr.getName());
                if (destFilenames == null || destFilenames.length != 1) {
                    //we only like simple mappers.
                    throw new BuildException(ERROR_BAD_MAP + fr.getFile());
                }

                File destFile = new File(toDir, destFilenames[0]);

                signOneJar(
                        fr.getFile(), destFile,
                        firstNonNull( jarsConfigsMap.get( fr.getName() ), defaultJarsConfigInternal ) );
            }
        }
        finally
        {
            endExecution();
        }
    }

    /**
     * Sign one jar.
     * <p/>
     * The signing only takes place if {@link #isUpToDate(File, File)} indicates
     * that it is needed.
     *
     * @param jarSource source to sign
     * @param jarTarget target; may be null
     * @throws BuildException
     */
    private void signOneJar( File jarSource, File jarTarget, JarConfigResolved jarConfig )
        throws BuildException
    {
        File targetFile = jarTarget;
        if ( targetFile == null )
        {
            targetFile = jarSource;
        }

        if ( isUpToDate( jarSource, targetFile ) )
        {
            return;
        }

        long lastModified = jarSource.lastModified();

        if ( destFlatten )
        {
            targetFile = new File( destDir, targetFile.getName() );
        }

        // +p -->

        boolean unsignFirstCombined = jarConfig.unsignFirst;

        Attributes manifestAttributes = jarConfig.extraManifestAttributes;

        File realSource = jarSource;
        File tmpJar = null;

        if ( unsignFirstCombined || ( ( manifestAttributes != null ) && !manifestAttributes.isEmpty() ) )
        {
            try
            {
                tmpJar = File.createTempFile( jarSource.getName(), ".tmp.jar" );

                tmpJar.deleteOnExit();

                JarUtils.archiveModifier(
                            jarSource, tmpJar,
                            null,
                            unsignFirstCombined,
                            manifestAttributes );

                jarSource = tmpJar;
            }
            catch ( IOException e )
            {
                throw new BuildException( e );
            }
        }

        if ( jarListener != null )
        {
            jarListener.beforeSigning( jarConfig );
        }

        // +p
        // pack200 -> REPACK
        if ( pack200 )
        {
            try
            {
                jarPack200.repack( jarSource );
            }
            catch ( IOException e )
            {
                throw new BuildException( "Unable to repack " + jarSource, e );
            }
        }

        // <-- +p

        for (int tries = 1; tries <= retryCount; tries++)
        {
            final ExecTask cmd = createJarSigner();

            setCommonOptions(cmd);

            bindToKeystore(cmd);
            if (null != sigfile) {
                addValue(cmd, "-sigfile");
                String value = this.sigfile;
                addValue(cmd, value);
            }

            try {
                //DO NOT SET THE -signedjar OPTION if source==dest
                //unless you like fielding hotspot crash reports
                if (!FILE_UTILS.areSame(jarSource, targetFile)) {
                    addValue(cmd, "-signedjar");
                    addValue(cmd, targetFile.getPath());
                }
            } catch (IOException ioex) {
                throw new BuildException(ioex);
            }

            if (internalsf) {
                addValue(cmd, "-internalsf");
            }

            if (sectionsonly) {
                addValue(cmd, "-sectionsonly");
            }

            if (sigAlg != null) {
                addValue(cmd, "-sigalg");
                addValue(cmd, sigAlg);
            }

            if (digestAlg != null) {
                addValue(cmd, "-digestalg");
                addValue(cmd, digestAlg);
            }

            //add -tsa operations if declared
            addTimestampAuthorityCommands(cmd);

            //JAR source is required
            addValue(cmd, jarSource.getPath());

            //alias is required for signing
            addValue(cmd, alias);

            log(
                "Signing archive"
                    + ( tries == 1
                            ? ""
                            : " (try:" + tries + ")" )
                    + ": "
                    + realSource.getAbsolutePath()
                    + " to "
                    + targetFile.getAbsolutePath()
                    + " as " + alias );

            try
            {
                cmd.execute();
            }
            catch (BuildException e)
            {
                log( e, Project.MSG_INFO );

                if ( tries == retryCount )
                {
                    throw e;
                }

                continue;
            }

            break;
        }

        // restore the lastModified attribute
        if ( preserveLastModified )
        {
            FILE_UTILS.setFileLastModified( targetFile, lastModified );
        }

        // +p
        // pack200 -> PACK
        if ( pack200 )
        {
            final File packedFile = jarPack200.getPackedFileFromJarFile( targetFile );

            try
            {
                jarPack200.pack( targetFile, packedFile );
            }
            catch ( IOException e )
            {
                throw new BuildException( "Unable to repack " + jarSource, e );
            }

            // restore the lastModified attribute
            if ( preserveLastModified )
            {
                FILE_UTILS.setFileLastModified( packedFile, lastModified );
            }
        }

        if ( tmpJar != null )
        {
            tmpJar.delete();
        }
    }

    /**
     * If the tsa parameters are set, this passes them to the command.
     * There is no validation of java version, as third party JDKs
     * may implement this on earlier/later jarsigner implementations.
     * @param cmd the exec task.
     */
    private void addTimestampAuthorityCommands(final ExecTask cmd) {
        if (tsaurl != null) {
            addValue(cmd, "-tsa");
            addValue(cmd, tsaurl);
        }
        if (tsacert != null) {
            addValue(cmd, "-tsacert");
            addValue(cmd, tsacert);
        }
    }

    /**
     * Compare a jar file with its corresponding signed jar. The logic for this
     * is complex, and best explained in the source itself. Essentially if
     * either file doesnt exist, or the destfile has an out of date timestamp,
     * then the return value is false.
     * <p/>
     * If we are signing ourself, the check {@link #isSigned(File)} is used to
     * trigger the process.
     *
     * @param jarFile       the unsigned jar file
     * @param signedjarFile the result signed jar file
     * @return true if the signedjarFile is considered up to date
     */
    protected boolean isUpToDate(File jarFile, File signedjarFile) {
        if (isForce() || null == jarFile || !jarFile.exists()) {
            //these are pathological cases, but retained in case somebody
            //subclassed us.
            return false;
        }

        //we normally compare destination with source
        File destFile = signedjarFile;
        if (destFile == null) {
            //but if no dest is specified, compare source to source
            destFile = jarFile;
        }

        //if, by any means, the destfile and source match,
        if (jarFile.equals(destFile)) {
            if (lazy) {
                //we check the presence of signatures on lazy signing
                return isSigned(jarFile);
            }
            //unsigned or non-lazy self signings are always false
            return false;
        }

        //if they are different, the timestamps are used
        return FILE_UTILS.isUpToDate(jarFile, destFile);
    }

    /**
     * test for a file being signed, by looking for a signature in the META-INF
     * directory with our alias/sigfile.
     *
     * @param file the file to be checked
     * @return true if the file is signed
     * @see IsSigned#isSigned(File, String)
     */
    protected boolean isSigned(File file) {
        try {
            return IsSigned.isSigned(file, sigfile == null ? alias : sigfile);
        } catch (IOException e) {
            //just log this
            log(e.toString(), Project.MSG_VERBOSE);
            return false;
        }
    }

    /**
     * true to indicate that the signed jar modification date remains the same
     * as the original. Defaults to false
     *
     * @param preserveLastModified if true preserve the last modified time
     */
    public void setPreserveLastModified(boolean preserveLastModified) {
        this.preserveLastModified = preserveLastModified;
    }

    private Attributes buildManifestAttributes( List<Property> properties )
    {
        Attributes manifestAttributes = null;

        if ( ( properties != null ) && !properties.isEmpty() )
        {
            manifestAttributes = new Attributes();

            for ( Property property : properties )
            {
                manifestAttributes.putValue( property.getName(), property.getValue() );
            }
        }

        return manifestAttributes;
    }

    private Map<String, JarConfigResolved> buildJarsConfigInternalMap(
                ResourceCollection sources, Attributes moreExtraManifestAttributes )
    {
        HashMap<String, JarConfigResolved> jarConfigResolvedMap =
                    new LinkedHashMap<String, JarConfigResolved>();

        if ( jarsConfigs != null )
        {
            for ( JarsConfig jarsConfig : jarsConfigs )
            {
                boolean unsignFirstCombined = firstNonNull( jarsConfig.getUnsignFirst(), unsignFirst );

                Attributes extraManifestAttributes =
                                buildManifestAttributes( jarsConfig.getExtraManifestAttributes() );

                if ( extraManifestAttributes == null )
                {
                    extraManifestAttributes = moreExtraManifestAttributes;
                }
                else if ( moreExtraManifestAttributes != null )
                {
                    extraManifestAttributes.putAll( moreExtraManifestAttributes );
                }

                for ( String attrToRemove : jarsConfig.getRemoveAttributes() )
                {
                    extraManifestAttributes.putValue( attrToRemove, null );
                }

                Restrict restrict = new Restrict();

                restrict.setProject( getProject() );

                restrict.add( sources );

                And patternFilter = new And();

                if ( jarsConfig.getIncludes() != null )
                {
                    Name includesFilter = new Name();
                    includesFilter.setName( jarsConfig.getIncludes() );

                    patternFilter.add( includesFilter );
                }

                if ( jarsConfig.getExcludes() != null )
                {
                    Name excludes = new Name();
                    excludes.setName( jarsConfig.getExcludes() );

                    Not excludesFilter = new Not();
                    excludesFilter.add( excludes );

                    patternFilter.add( excludesFilter );
                }

                if ( patternFilter.hasSelectors() )
                {
                    restrict.add( patternFilter );
                }

                Iterator<Resource> iter = restrict.iterator();
                while ( iter.hasNext() )
                {
                    Resource resource = iter.next();

                    FileResource fr = ResourceUtils
                            .asFileResource( resource.as( FileProvider.class ) );

                    if ( getBasedir() != null )
                    {
                        fr.setBaseDir( getBasedir() );
                    }

                    jarConfigResolvedMap.put(
                        fr.getName(),
                        new JarConfigResolved( unsignFirstCombined, extraManifestAttributes ) );
               }
            }
        }

        return jarConfigResolvedMap;
    }
}
