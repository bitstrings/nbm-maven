/*
 *  Copyright 2008 Johan Andrén.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.codehaus.mojo.nbm;

import static org.codehaus.mojo.nbm.utils.JarUtils.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.GenerateKey;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Jar.FilesetManifestConfig;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.taskdefs.SignJar;
import org.apache.tools.ant.taskdefs.Taskdef;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.codehaus.mojo.nbm.JarsConfig.ManifestEntries;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.netbeans.nbbuild.MakeJnlp2;
import org.netbeans.nbbuild.ModuleSelector;
import org.netbeans.nbbuild.VerifyJNLP;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.google.common.base.Joiner;

/**
 * Create webstartable binaries for a 'nbm-application'.
 * @author <a href="mailto:johan.andren@databyran.se">Johan Andrén</a>
 * @author <a href="mailto:mkleint@codehaus.org">Milos Kleint</a>
 * @since 3.0
 */
//
// +p
// this crap should be refactored
//
@Mojo(name="webstart-app", defaultPhase= LifecyclePhase.PACKAGE )
public class CreateWebstartAppMojo
    extends AbstractNbmMojo
{
    /**
     * The Maven project.
     */
    @org.apache.maven.plugins.annotations.Parameter(required=true, readonly=true, property="project")
    private MavenProject project;

    @Component
    protected MavenProjectHelper projectHelper;

    /**
     * The branding token for the application based on NetBeans platform.
     */
    @org.apache.maven.plugins.annotations.Parameter(required=true, property="netbeans.branding.token")
    protected String brandingToken;

    /**
     * output directory where the the NetBeans application will be created.
     */
    @org.apache.maven.plugins.annotations.Parameter(required=true, defaultValue="${project.build.directory}")
    private File outputDirectory;

    /**
     * Ready-to-deploy WAR containing application in JNLP packaging.
     *
     */
    @org.apache.maven.plugins.annotations.Parameter(required=true, defaultValue="${project.build.directory}/${project.artifactId}-${project.version}-jnlp.war")
    private File destinationFile;

    /**
     * Artifact Classifier to use for the webstart distributable zip file.
     * @since 3.1
     */
    @org.apache.maven.plugins.annotations.Parameter(defaultValue="webstart", property="nbm.webstart.classifier")
    private String webstartClassifier;

    /**
     * Codebase value within *.jnlp files.
     * <strong>Defining this parameter is generally a bad idea.</strong>
     */
    @org.apache.maven.plugins.annotations.Parameter(property="nbm.webstart.codebase")
    private String codebase;

    /**
     * A custom master JNLP file. If not defined, the
     * <a href="http://mojo.codehaus.org/nbm-maven-plugin/masterjnlp.txt">default one</a> is used.
     * The following expressions can be used within the file and will
     * be replaced when generating content.
     * <ul>
     * <li>${jnlp.resources}</li>
     * <li>${jnlp.codebase} - the 'codebase' parameter value is passed in.</li>
     * <li>${app.name}</li>
     * <li>${app.title}</li>
     * <li>${app.vendor}</li>
     * <li>${app.description}</li>
     * <li>${branding.token} - the 'brandingToken' parameter value is passed in.</li>
     * <li>${netbeans.jnlp.fixPolicy}</li>
     * </ul>
     */
    @org.apache.maven.plugins.annotations.Parameter
    private File masterJnlpFile;

    /**
     * The basename (minus .jnlp extension) of the master JNLP file in the output.
     * This file will be the entry point for javaws.
     * Defaults to the branding token.
     * @since 3.5
     */
    @org.apache.maven.plugins.annotations.Parameter(property="master.jnlp.file.name")
    private String masterJnlpFileName;

    /**
     * keystore location for signing the nbm file
     */
    @org.apache.maven.plugins.annotations.Parameter(property="keystore")
    private String keystore;

    /**
     * keystore password
     */
    @org.apache.maven.plugins.annotations.Parameter(property="keystorepass")
    private String keystorepassword;

    /**
     * keystore alias
     */
    @org.apache.maven.plugins.annotations.Parameter(property="keystorealias")
    private String keystorealias;

    /**
     * keystore type
     * @since 3.5
     */
    @org.apache.maven.plugins.annotations.Parameter(property="keystoretype")
    private String keystoretype;

    /**
     * If set true, build-jnlp target creates versioning info in jnlp descriptors and version.xml files.
     * This allows for incremental updates of Webstart applications, but requires download via
     * JnlpDownloadServlet
     * Defaults to false, which means versioning
     * info is not generated (see
     * http://java.sun.com/j2se/1.5.0/docs/guide/javaws/developersguide/downloadservletguide.html#resources).
     *
     */
    @org.apache.maven.plugins.annotations.Parameter(defaultValue="false", property="nbm.webstart.versions")
    private boolean processJarVersions;

    // +p
    @org.apache.maven.plugins.annotations.Parameter( defaultValue="false", property="nbm.webstart.signWar" )
    private boolean signWar;

    // +p
    @org.apache.maven.plugins.annotations.Parameter(
                defaultValue="false", property="nbm.webstart.generateJnlpApplicationTemplate" )
    private boolean generateJnlpApplicationTemplate;

    /**
     * additional command line arguments. Eg.
     * -J-Xdebug -J-Xnoagent -J-Xrunjdwp:transport=dt_socket,suspend=n,server=n,address=8888
     * can be used to debug the IDE.
     */
    // +p
    @org.apache.maven.plugins.annotations.Parameter( property="netbeans.run.params" )
    private String additionalArguments;

    // +p
    @org.apache.maven.plugins.annotations.Parameter( property="nbm.signing.threads", defaultValue="0" )
    private int signingThreads;

    // +p
    @org.apache.maven.plugins.annotations.Parameter( property="nbm.signing.force", defaultValue="true" )
    private boolean signingForce;

    // +p
    @org.apache.maven.plugins.annotations.Parameter( property="nbm.signing.tsacert" )
    private String signingTsaCert;

    // +p
    @org.apache.maven.plugins.annotations.Parameter( property="nbm.signing.tsaurl" )
    private String signingTsaUrl;

    // +p
    @org.apache.maven.plugins.annotations.Parameter(
                        property="nbm.signing.removeExistingSignatures",
                        defaultValue="false")
    private boolean signingRemoveExistingSignatures;

    // +p
    @org.apache.maven.plugins.annotations.Parameter( property="nbm.signing.maxMemory" )
    private String signingMaxMemory = "96m";

    // +p
    @org.apache.maven.plugins.annotations.Parameter(
                        property="nbm.signing.retryCount",
                        defaultValue="5")
    private int signingRetryCount;

    @org.apache.maven.plugins.annotations.Parameter(
                property="encoding", defaultValue="${project.build.sourceEncoding}" )
    private String encoding;

    @org.apache.maven.plugins.annotations.Parameter( property="session", readonly=true, required=true )
    private MavenSession session;

    // +p
    @Component
    private MavenResourcesFiltering mavenResourcesFiltering;

    // +p
    @org.apache.maven.plugins.annotations.Parameter( defaultValue="true" )
    private boolean autoManifestSecurityEntries;

    // +p
    @org.apache.maven.plugins.annotations.Parameter
    private List<JarsConfig> jarsConfigs;

    // +p
    @org.apache.maven.plugins.annotations.Parameter
    private List<Resource> webappResources;

    // +p
    private String jarPermissions;
    private String jarCodebase;
    private String jnlpSecurity;

    /**
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     * @throws org.apache.maven.plugin.MojoFailureException
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( signingThreads < 1 )
        {
            signingThreads = Runtime.getRuntime().availableProcessors();
        }

        getLog().info( "Using " + signingThreads + " signing threads." );

        if ( !"nbm-application".equals( project.getPackaging() ) )
        {
            throw new MojoExecutionException(
                "This goal only makes sense on project with nbm-application packaging." );
        }

        final Project antProject = antProject();

        getLog().warn( "WARNING: Unsigned and self-signed WebStart applications are deprecated from JDK7u21 onwards. To ensure future correct functionality please use trusted certificate.");

        if ( keystore != null && keystorealias != null && keystorepassword != null )
        {
            File ks = new File( keystore );
            if ( !ks.exists() )
            {
                throw new MojoFailureException( "Cannot find keystore file at " + ks.getAbsolutePath() );
            }
            else
            {
                //proceed..
            }
        }
        else if ( keystore != null || keystorepassword != null || keystorealias != null )
        {
            throw new MojoFailureException(
                "If you want to sign the jnlp application, you need to define all three keystore related parameters." );
        }
        else
        {
            File generatedKeystore = new File( outputDirectory, "generated.keystore" );
            if ( ! generatedKeystore.exists() )
            {
                getLog().warn( "Keystore related parameters not set, generating a default keystore." );
                GenerateKey genTask = (GenerateKey) antProject.createTask( "genkey" );
                genTask.setAlias( "jnlp" );
                genTask.setStorepass( "netbeans" );
                genTask.setDname( "CN=" + System.getProperty( "user.name" ) );
                genTask.setKeystore( generatedKeystore.getAbsolutePath() );
                genTask.execute();
            }
            keystore = generatedKeystore.getAbsolutePath();
            keystorepassword = "netbeans";
            keystorealias = "jnlp";
        }

        Taskdef taskdef = (Taskdef) antProject.createTask( "taskdef" );
        taskdef.setClassname( MakeJnlp2.class.getName() );
        taskdef.setName( "makejnlp" );
        taskdef.execute();

        taskdef = (Taskdef) antProject.createTask( "taskdef" );
        taskdef.setClassname( Jar.class.getName() );
        taskdef.setName( "jar" );
        taskdef.execute();

        taskdef = (Taskdef) antProject.createTask( "taskdef" );
        taskdef.setClassname( VerifyJNLP.class.getName() );
        taskdef.setName( "verifyjnlp" );
        taskdef.execute();
        // +p

        try
        {
            final File webstartBuildDir =
                            new File( outputDirectory + File.separator + "webstart" + File.separator + brandingToken );

            if ( webstartBuildDir.exists() )
            {
                FileUtils.deleteDirectory( webstartBuildDir );
            }

            webstartBuildDir.mkdirs();

            // P: copy webappResources --[

            MavenResourcesExecution mavenResourcesExecution =
                new MavenResourcesExecution(
                            webappResources,
                            webstartBuildDir,
                            project, encoding,
                            Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                            session );
            mavenResourcesExecution.setEscapeWindowsPaths( true );
            mavenResourcesFiltering.filterResources( mavenResourcesExecution );

            // ]--


            final String localCodebase = codebase != null ? codebase : webstartBuildDir.toURI().toString();
            getLog().info( "Generating webstartable binaries at " + webstartBuildDir.getAbsolutePath() );

            final File nbmBuildDirFile = new File( outputDirectory, brandingToken );

            // +p (needs to be before make jnlp)

            //TODO is it really netbeans/
            if ( masterJnlpFileName == null )
            {
               masterJnlpFileName = brandingToken;
            }

            Properties props = new Properties();
            props.setProperty( "jnlp.codebase", localCodebase );
            props.setProperty( "app.name", brandingToken );
            props.setProperty( "app.title", project.getName() );
            if ( project.getOrganization() != null )
            {
                props.setProperty( "app.vendor", project.getOrganization().getName() );
            }
            else
            {
                props.setProperty( "app.vendor", "Nobody" );
            }
            String description = project.getDescription() != null ? project.getDescription() : "No Project Description";
            props.setProperty( "app.description", description );
            props.setProperty( "branding.token", brandingToken );
            props.setProperty( "master.jnlp.file.name", masterJnlpFileName );
            props.setProperty( "netbeans.jnlp.fixPolicy", "false" );

            StringBuilder stBuilder = new StringBuilder();
            if ( additionalArguments != null )
            {
                StringTokenizer st = new StringTokenizer( additionalArguments );
                while ( st.hasMoreTokens() )
                {
                    String arg = st.nextToken();
                    if ( arg.startsWith( "-J" ) )
                    {
                        if ( stBuilder.length() > 0 )
                        {
                            stBuilder.append( ' ' );
                        }
                        stBuilder.append( arg.substring( 2 ) );
                    }
                }
            }
            props.setProperty( "netbeans.run.params", stBuilder.toString() );

            File masterJnlp = new File(
                webstartBuildDir.getAbsolutePath() + File.separator + masterJnlpFileName + ".jnlp" );
            filterCopy( masterJnlpFile, "master.jnlp", masterJnlp, props );


            File startup = copyLauncher( outputDirectory, nbmBuildDirFile );

            String masterJnlpStr = FileUtils.fileRead( masterJnlp );

            // P: JNLP-INF/APPLICATION_TEMPLATE.JNLP support --[
            // this can be done better and will
            // ashamed
            if ( generateJnlpApplicationTemplate )
            {
                File jnlpInfDir = new File( outputDirectory, "JNLP-INF" );

                getLog().info( "Generate JNLP application template under: " + jnlpInfDir );

                jnlpInfDir.mkdirs();

                File jnlpTemplate = new File( jnlpInfDir, "APPLICATION_TEMPLATE.JNLP" );

                masterJnlpStr = masterJnlpStr
                                    .replaceAll( "(<jnlp.*codebase\\ *=\\ *)\"((?!\").)*", "$1\"*" )
                                    .replaceAll( "(<jnlp.*href\\ *=\\ *)\"((?!\").)*", "$1\"*" );

                FileUtils.fileWrite( jnlpTemplate, masterJnlpStr );

                File startupMerged = new File( outputDirectory, "startup-jnlpinf.jar" );

                Jar jar = (Jar) antProject.createTask( "jar" );
                jar.setDestFile( startupMerged );
                jar.setFilesetmanifest(
                            (FilesetManifestConfig) EnumeratedAttribute.getInstance(
                                        FilesetManifestConfig.class,
                                        "merge" ) );

                FileSet jnlpInfDirectoryFileSet = new FileSet();
                jnlpInfDirectoryFileSet.setDir( outputDirectory );
                jnlpInfDirectoryFileSet.setIncludes( "JNLP-INF/**" );

                jar.addFileset( jnlpInfDirectoryFileSet );

                ZipFileSet startupJar = new ZipFileSet();
                startupJar.setSrc( startup );

                jar.addZipfileset( startupJar );

                jar.execute();

                startup = startupMerged;

                getLog().info( "APPLICATION_TEMPLATE.JNLP generated - startup.jar: " + startup );
            }

            final JarsConfig startupConfig = new JarsConfig();

            ManifestEntries startupManifestEntries = new ManifestEntries();

            startupConfig.setManifestEntries( startupManifestEntries );

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse( new InputSource( new StringReader( masterJnlpStr ) ) );

            Element jnlpRoot = doc.getDocumentElement();

            jarCodebase = jnlpRoot.getAttribute( "codebase" );

            if ( jarCodebase.isEmpty() )
            {
                jarCodebase = "*";
            }

            startupManifestEntries.setCodebase( jarCodebase );

            XPath xpath = XPathFactory.newInstance().newXPath();

            Node jnlpSecurityPermission =
                    (Node) xpath.evaluate(
                            "(/jnlp/security/all-permissions | /jnlp/security/j2ee-application-client-permissions)[1]",
                            doc,
                            XPathConstants.NODE );

            if ( jnlpSecurityPermission == null )
            {
                jarPermissions = "sandbox";
                jnlpSecurity = "";
            }
            else
            {
                jarPermissions = "all-permissions";
                jnlpSecurity = "<security><" + jnlpSecurityPermission.getNodeName() + "/></security>";
            }

            startupManifestEntries.setPermissions( jarPermissions );

            startupManifestEntries.setApplicationName( brandingToken );

            // +p

            if ( autoManifestSecurityEntries )
            {
                if ( jarsConfigs == null )
                {
                    jarsConfigs = new ArrayList<JarsConfig>();
                }

                jarsConfigs.add( 0, startupConfig );
            }

            final List<SignJar.JarsConfig> signJarJarsConfigs = buildSignJarJarsConfigs( jarsConfigs );

            File jnlpDestination = new File(
                webstartBuildDir.getAbsolutePath() + File.separator + "startup.jar" );

            SignJar signTask = (SignJar) antProject.createTask( "signjar" );
            signTask.setKeystore( keystore );
            signTask.setStorepass( keystorepassword );
            signTask.setAlias( keystorealias );

            if ( keystoretype != null )
            {
                signTask.setStoretype( keystoretype );
            }

            signTask.setForce( signingForce );
            signTask.setTsacert( signingTsaCert );
            signTask.setTsaurl( signingTsaUrl );
            signTask.setMaxmemory( signingMaxMemory );
            signTask.setRetryCount( signingRetryCount );

            signTask.setUnsignFirst( signingRemoveExistingSignatures );

            signTask.setJarsConfigs( buildSignJarJarsConfigs( Collections.singletonList( startupConfig ) ) );

            signTask.setBasedir( nbmBuildDirFile );

            signTask.setSignedjar( jnlpDestination );

            signTask.setJar( startup );

            signTask.execute();
            // <-- all of this will be refactored soon ]--


//            FileUtils.copyDirectoryStructureIfModified( nbmBuildDirFile, webstartBuildDir );


            MakeJnlp2 jnlpTask = (MakeJnlp2) antProject.createTask( "makejnlp" );
            jnlpTask.setDir( webstartBuildDir );
            jnlpTask.setCodebase( localCodebase );
            //TODO, how to figure verify excludes..
            jnlpTask.setVerify( false );
            jnlpTask.setPermissions( jnlpSecurity );
            jnlpTask.setSignJars( true );

            jnlpTask.setAlias( keystorealias );
            jnlpTask.setKeystore( keystore );
            jnlpTask.setStorePass( keystorepassword );
            if ( keystoretype != null )
            {
                jnlpTask.setStoreType( keystoretype );
            }

            jnlpTask.setSigningForce( signingForce );
            jnlpTask.setSigningTsaCert( signingTsaCert );
            jnlpTask.setSigningTsaUrl( signingTsaUrl );
            jnlpTask.setUnsignFirst( signingRemoveExistingSignatures );
            jnlpTask.setJarsConfigs( signJarJarsConfigs );
            jnlpTask.setSigningMaxMemory( signingMaxMemory );
            jnlpTask.setSigningRetryCount( signingRetryCount );
            jnlpTask.setBasedir( nbmBuildDirFile );

            jnlpTask.setNbThreads( signingThreads );

            jnlpTask.setProcessJarVersions( processJarVersions );

            FileSet fs = jnlpTask.createModules();
            fs.setDir( nbmBuildDirFile );
            OrSelector or = new OrSelector();
            AndSelector and = new AndSelector();
            FilenameSelector inc = new FilenameSelector();
            inc.setName( "*/modules/**/*.jar" );
            or.addFilename( inc );
            inc = new FilenameSelector();
            inc.setName( "*/lib/**/*.jar" );
            or.addFilename( inc );
            inc = new FilenameSelector();
            inc.setName( "*/core/**/*.jar" );
            or.addFilename( inc );

            ModuleSelector ms = new ModuleSelector();
            Parameter included = new Parameter();
            included.setName( "includeClusters" );
            included.setValue( "" );
            Parameter excluded = new Parameter();
            excluded.setName( "excludeClusters" );
            excluded.setValue( "" );
            Parameter exModules = new Parameter();
            exModules.setName( "excludeModules" );
            exModules.setValue( "" );
            ms.setParameters( new Parameter[]
                {
                    included,
                    excluded,
                    exModules
                } );
            and.add( or );
            and.add( ms );
            fs.addAnd( and );
            jnlpTask.execute();

            String extSnippet = generateExtensions( fs, antProject, "" ); // "netbeans/"

            //branding
            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir( nbmBuildDirFile );
            ds.setIncludes( new String[]
                {
                    "**/locale/*.jar"
                } );
            ds.scan();
            String[] includes = ds.getIncludedFiles();
            StringBuilder brandRefs = new StringBuilder();

            if ( includes != null && includes.length > 0 )
            {
                final File brandingDir = new File( webstartBuildDir, "branding" );
                brandingDir.mkdirs();
                for ( String incBran : includes )
                {
                    File source = new File( nbmBuildDirFile, incBran );
                    File dest = new File( brandingDir, source.getName() );
                    brandRefs.append( "    <jar href=\'branding/" ).append( dest.getName() ).append( "\'/>\n" );
                }

                final ExecutorService executorService = Executors.newFixedThreadPool( signingThreads );

                final List<Exception> threadException = new ArrayList<Exception>();

                for (final String toSign : includes)
                {
                    executorService.execute( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                File toSignFile = new File( nbmBuildDirFile, toSign );

                                SignJar signTask = (SignJar) antProject.createTask( "signjar" );
                                signTask.setKeystore( keystore );
                                signTask.setStorepass( keystorepassword );
                                signTask.setAlias( keystorealias );
                                signTask.setForce( signingForce );
                                signTask.setTsacert( signingTsaCert );
                                signTask.setTsaurl( signingTsaUrl );
                                signTask.setMaxmemory( signingMaxMemory );
                                signTask.setRetryCount( signingRetryCount );
                                signTask.setUnsignFirst( signingRemoveExistingSignatures );
                                signTask.setJarsConfigs( signJarJarsConfigs );
                                signTask.setJar( toSignFile );
                                signTask.setDestDir( brandingDir );
                                signTask.setBasedir( nbmBuildDirFile );
                                signTask.setDestFlatten( true );
                                signTask.execute();
                            }
                            catch ( Exception e )
                            {
                                threadException.add( e );
                            }
                        }
                    } );
                }

                executorService.shutdown();

                executorService.awaitTermination( Long.MAX_VALUE, TimeUnit.NANOSECONDS );

                if ( !threadException.isEmpty() )
                {
                    throw threadException.get( 0 );
                }
            }

            File modulesJnlp = new File(
                webstartBuildDir.getAbsolutePath() + File.separator + "modules.jnlp" );
            props.setProperty( "jnlp.branding.jars", brandRefs.toString() );
            props.setProperty( "jnlp.resources", extSnippet );
            filterCopy( null, /* filename is historical */"branding.jnlp", modulesJnlp, props );

            getLog().info( "Verifying generated webstartable content." );
            VerifyJNLP verifyTask = (VerifyJNLP) antProject.createTask( "verifyjnlp" );
            FileSet verify = new FileSet();
            verify.setFile( masterJnlp );
            verifyTask.addConfiguredFileset( verify );
            verifyTask.execute();


            // create zip archive
            if ( destinationFile.exists() )
            {
                destinationFile.delete();
            }
            ZipArchiver archiver = new ZipArchiver();
            if ( codebase != null )
            {
                getLog().warn( "Defining <codebase>/${nbm.webstart.codebase} is generally unnecessary" );
                archiver.addDirectory( webstartBuildDir );
            }
            else
            {
                archiver.addDirectory( webstartBuildDir, null, new String[] { "**/*.jnlp" } );
                for ( final File jnlp : webstartBuildDir.listFiles() )
                {
                    if ( !jnlp.getName().endsWith( ".jnlp" ) )
                    {
                        continue;
                    }
                    archiver.addResource( new PlexusIoResource() {
                        public @Override InputStream getContents() throws IOException
                        {
                            return new ByteArrayInputStream( FileUtils.fileRead( jnlp, "UTF-8" ).replace( localCodebase, "$$codebase" ).getBytes( "UTF-8" ) );
                        }
                        public @Override long getLastModified()
                        {
                            return jnlp.lastModified();
                        }
                        public @Override boolean isExisting()
                        {
                            return true;
                        }
                        public @Override long getSize()
                        {
                            return UNKNOWN_RESOURCE_SIZE;
                        }
                        public @Override URL getURL() throws IOException
                        {
                            return null;
                        }
                        public @Override String getName()
                        {
                            return jnlp.getAbsolutePath();
                        }
                        public @Override boolean isFile()
                        {
                            return true;
                        }
                        public @Override boolean isDirectory()
                        {
                            return false;
                        }
                    }, jnlp.getName(), archiver.getDefaultFileMode() );
                }
            }
            File jdkhome = new File( System.getProperty( "java.home" ) );
            File servlet = new File( jdkhome, "sample/jnlp/servlet/jnlp-servlet.jar" );
            if ( ! servlet.exists() )
            {
                servlet = new File( jdkhome.getParentFile(), "sample/jnlp/servlet/jnlp-servlet.jar" );

                if ( !servlet.exists() )
                {
                    servlet = File.createTempFile( "nbm_", "jnlp-servlet.jar" );

                    FileUtils.copyURLToFile(
                            Thread.currentThread().getContextClassLoader()
                                    .getResource( "jnlp-servlet.jar" ),
                            servlet );
                }
            }
            if ( servlet.exists() )
            {
                archiver.addFile( servlet, "WEB-INF/lib/jnlp-servlet.jar" );
                archiver.addResource( new PlexusIoResource() {
                    public @Override InputStream getContents() throws IOException
                    {
                        return new ByteArrayInputStream( ( "" +
                            "<web-app>\n" +
                            "    <servlet>\n" +
                            "        <servlet-name>JnlpDownloadServlet</servlet-name>\n" +
                            "        <servlet-class>jnlp.sample.servlet.JnlpDownloadServlet</servlet-class>\n" +
                            "    </servlet>\n" +
                            "    <servlet-mapping>\n" +
                            "        <servlet-name>JnlpDownloadServlet</servlet-name>\n" +
                            "        <url-pattern>*.jnlp</url-pattern>\n" +
                            "    </servlet-mapping>\n" +
                            "</web-app>\n" ).getBytes() );
                    }
                    public @Override long getLastModified()
                    {
                        return UNKNOWN_MODIFICATION_DATE;
                    }
                    public @Override boolean isExisting()
                    {
                        return true;
                    }
                    public @Override long getSize()
                    {
                        return UNKNOWN_RESOURCE_SIZE;
                    }
                    public @Override URL getURL() throws IOException
                    {
                        return null;
                    }
                    public @Override String getName()
                    {
                        return "web.xml";
                    }
                    public @Override boolean isFile()
                    {
                        return true;
                    }
                    public @Override boolean isDirectory()
                    {
                        return false;
                    }
                }, "WEB-INF/web.xml", archiver.getDefaultFileMode() );
            }
            archiver.setDestFile( destinationFile );
            archiver.createArchive();

            if (signWar)
            {
                signTask = (SignJar) antProject.createTask( "signjar" );
                signTask.setKeystore( keystore );
                signTask.setStorepass( keystorepassword );
                signTask.setAlias( keystorealias );
                signTask.setForce( signingForce );
                signTask.setTsacert( signingTsaCert );
                signTask.setTsaurl( signingTsaUrl );
                signTask.setMaxmemory( signingMaxMemory );
                signTask.setRetryCount( signingRetryCount );
                signTask.setJar( destinationFile );
                signTask.execute();
            }

            // attach standalone so that it gets installed/deployed
            projectHelper.attachArtifact( project, "war", webstartClassifier, destinationFile );

        }
        catch ( Exception ex )
        {
            throw new MojoExecutionException( "Error creating webstartable binary.", ex );
        }
    }

    /**
     * @param standaloneBuildDir
     * @return The name of the jnlp-launcher jarfile in the build directory
     */
    private File copyLauncher( File standaloneBuildDir, File builtInstallation )
        throws IOException
    {
        File jnlpStarter =
            new File( builtInstallation.getAbsolutePath() + File.separator + "harness" + File.separator + "jnlp"
                + File.separator + "jnlp-launcher.jar" );
        // buffer so it isn't reading a byte at a time!
        InputStream source = null;
        FileOutputStream outstream = null;
        try
        {
            if ( !jnlpStarter.exists() )
            {
                source = getClass().getClassLoader().getResourceAsStream(
                    "harness/jnlp/jnlp-launcher.jar" );
            }
            else
            {
                source = new FileInputStream( jnlpStarter );
            }
            File jnlpDestination = new File(
                standaloneBuildDir.getAbsolutePath() + File.separator + "jnlp-launcher.jar" );

            outstream = new FileOutputStream( jnlpDestination );
            IOUtil.copy( source, outstream );
            return jnlpDestination;
        }
        finally
        {
            IOUtil.close( source );
            IOUtil.close( outstream );
        }
    }

    private void filterCopy( File sourceFile, String resourcePath, File destinationFile, Properties filterProperties )
        throws IOException
    {
        // buffer so it isn't reading a byte at a time!
        Reader source = null;
        Writer destination = null;
        try
        {
            InputStream instream;
            if ( sourceFile != null )
            {
                instream = new FileInputStream( sourceFile );
            }
            else
            {
                instream = getClass().getClassLoader().getResourceAsStream( resourcePath );
            }
            FileOutputStream outstream = new FileOutputStream( destinationFile );

            source = new BufferedReader( new InputStreamReader( instream, "UTF-8" ) );
            destination = new OutputStreamWriter( outstream, "UTF-8" );

            // support ${token}
            Reader reader = new InterpolationFilterReader( source, filterProperties, "${", "}" );

            IOUtil.copy( reader, destination );
        }
        finally
        {
            IOUtil.close( source );
            IOUtil.close( destination );
        }
    }

    /**
     * copied from MakeMasterJNLP ant task.
     * @param files
     * @param antProject
     * @param masterPrefix
     * @return
     * @throws java.io.IOException
     */
    private String generateExtensions( FileSet files, Project antProject, String masterPrefix )
        throws IOException
    {
        StringBuilder buff = new StringBuilder();
        for ( String nm : files.getDirectoryScanner( antProject ).getIncludedFiles() )
        {
            File jar = new File( files.getDir( antProject ), nm );

            if ( !jar.canRead() )
            {
                throw new IOException( "Cannot read file: " + jar );
            }

            JarFile theJar = new JarFile( jar );
            String codenamebase = theJar.getManifest().getMainAttributes().getValue( "OpenIDE-Module" );
            if ( codenamebase == null )
            {
                throw new IOException( "Not a NetBeans Module: " + jar );
            }
            {
                int slash = codenamebase.indexOf( '/' );
                if ( slash >= 0 )
                {
                    codenamebase = codenamebase.substring( 0, slash );
                }
            }
            String dashcnb = codenamebase.replace( '.', '-' );

            buff.append( "    <extension name='" ).append( codenamebase ).append( "' href='" ).append( masterPrefix ).append( dashcnb ).append( ".jnlp' />\n" );
            theJar.close();
        }
        return buff.toString();

    }

    private List<SignJar.JarsConfig> buildSignJarJarsConfigs( List<JarsConfig> jarsConfigs )
    {
        List<SignJar.JarsConfig> signJarJarsConfigs = new ArrayList<SignJar.JarsConfig>();

        if ( jarsConfigs != null )
        {
            for ( JarsConfig jarsConfig : jarsConfigs )
            {
                SignJar.JarsConfig signJarJarsConfig = new SignJar.JarsConfig();

                if ( jarsConfig.getJarSet() != null )
                {
                    signJarJarsConfig.setIncludes( Joiner.on(',').join( jarsConfig.getJarSet().getIncludes() ) );
                    signJarJarsConfig.setExcludes( Joiner.on(',').join( jarsConfig.getJarSet().getExcludes() ) );
                }

                signJarJarsConfig.setUnsignFirst( jarsConfig.getRemoveExistingSignatures() );

                List<Property> signJarManifestAttributes = new ArrayList<Property>();

                JarsConfig.ManifestEntries manifestEntries = jarsConfig.getManifestEntries();

                if ( manifestEntries != null )
                {
                    if ( manifestEntries.getTrustedOnly() != null )
                    {
                        signJarManifestAttributes
                            .add(
                                createAntProperty(
                                        MANIFEST_ATTR_TRUSTED_ONLY,
                                        manifestEntries.getTrustedOnly().toString() ) );
                    }

                    if ( manifestEntries.getTrustedLibrary() != null )
                    {
                        signJarManifestAttributes
                            .add(
                                createAntProperty(
                                        MANIFEST_ATTR_TRUSTED_LIBRARY,
                                        manifestEntries.getTrustedLibrary().toString() ) );
                    }

                    if ( manifestEntries.getPermissions() != null )
                    {
                        signJarManifestAttributes
                            .add(
                                createAntProperty(
                                        MANIFEST_ATTR_PERMISSIONS,
                                        manifestEntries.getPermissions() ) );
                    }

                    if ( manifestEntries.getCodebase() != null )
                    {
                        signJarManifestAttributes
                            .add(
                                createAntProperty(
                                        MANIFEST_ATTR_CODEBASE,
                                        manifestEntries.getCodebase() ) );
                    }

                    if ( manifestEntries.getApplicationName() != null )
                    {
                        signJarManifestAttributes
                            .add(
                                createAntProperty(
                                        MANIFEST_ATTR_APPLICATION_NAME,
                                        manifestEntries.getApplicationName() ) );
                    }

                    Map<String, String> jarsConfigManifestAttributes = manifestEntries.getExtraAttributes();

                    if ( jarsConfigManifestAttributes != null )
                    {
                        for ( Map.Entry<String, String> entry : jarsConfigManifestAttributes.entrySet() )
                        {
                            signJarManifestAttributes.add(
                                        createAntProperty( entry.getKey(), entry.getValue() ) );
                        }
                    }
                }

                if ( autoManifestSecurityEntries )
                {
                    if ( manifestEntries == null )
                    {
                        manifestEntries = new ManifestEntries();
                    }

                    if ( manifestEntries.getPermissions() == null )
                    {
                        signJarManifestAttributes
                                .add(
                                    createAntProperty(
                                            MANIFEST_ATTR_PERMISSIONS,
                                            jarPermissions ) );
                    }

                    if ( manifestEntries.getCodebase() == null )
                    {
                        signJarManifestAttributes
                                .add(
                                    createAntProperty(
                                            MANIFEST_ATTR_CODEBASE,
                                            jarCodebase ) );
                    }

                    if ( manifestEntries.getApplicationName() == null )
                    {
                        signJarManifestAttributes
                                .add(
                                    createAntProperty(
                                            MANIFEST_ATTR_APPLICATION_NAME,
                                            brandingToken ) );
                    }
                }

                signJarJarsConfig.setExtraManifestAttributes( signJarManifestAttributes );

                signJarJarsConfigs.add( signJarJarsConfig );
            }
        }

        return signJarJarsConfigs;
    }

    private Property createAntProperty( String name, String value )
    {
        Property property = new Property();
        property.setProject( antProject() );
        property.setName( name );
        property.setValue( value );
        property.execute();

        return property;
    }
}
