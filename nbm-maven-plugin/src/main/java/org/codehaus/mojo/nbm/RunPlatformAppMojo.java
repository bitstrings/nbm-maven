/* ==========================================================================
 * Copyright 2007 Mevenide Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * =========================================================================
 */
package org.codehaus.mojo.nbm;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Run a branded application on top of NetBeans Platform. To be used with projects
 * with nbm-application packaging only and the project needs to be built first.
 * @author <a href="mailto:mkleint@codehaus.org">Milos Kleint</a>
 *
 */
@Mojo(name="run-platform", requiresDependencyResolution= ResolutionScope.RUNTIME )
public class RunPlatformAppMojo
        extends AbstractMojo
{

    /**
     * The branding token for the application based on NetBeans platform.
     */
    @Parameter(required=true, property="netbeans.branding.token")
    protected String brandingToken;
    /**
     * output directory where the the NetBeans application is created.
     */
    @Parameter(required=true, defaultValue="${project.build.directory}")
    private File outputDirectory;

    /**
     * NetBeans user directory for the executed instance.
     */
    @Parameter(required=true, defaultValue="${project.build.directory}/userdir", property="netbeans.userdir")
    protected File netbeansUserdir;
    /**
     * additional command line arguments passed to the application.
     * can be used to debug the IDE.
     */
    @Parameter(property="netbeans.run.params")
    protected String additionalArguments;
    
    /**
     * Attach a debugger to the application JVM. If set to "true", the process will suspend and wait for a debugger to attach
     * on port 5005. If set to some other string, that string will be appended to the <code>additionalArguments</code>, allowing you to configure
     * arbitrary debug-ability options (without overwriting the other options specified through the <code>additionalArguments</code>
     * parameter).
     * @since 3.11
     */
    @Parameter(property="netbeans.run.params.debug")
    protected String debugAdditionalArguments;
    
    /**
     * The Maven Project.
     *
     */
    @Parameter(required=true, readonly=true, property="project")
    private MavenProject project;

    /**
     *
     * @throws MojoExecutionException if an unexpected problem occurs
     * @throws MojoFailureException if an expected problem occurs
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !"nbm-application".equals( project.getPackaging() ) )
        {
            throw new MojoFailureException( "The nbm:run-platform goal shall be used within a NetBeans Application project only ('nbm-application' packaging)");
        }

        netbeansUserdir.mkdirs();

        File appbasedir = new File( outputDirectory, brandingToken );

        if ( !appbasedir.exists() )
        {
            throw new MojoExecutionException( "The directory that shall contain built application, doesn't exist ("
                + appbasedir.getAbsolutePath() + ")\n Please invoke 'mvn install' on the project first" );
        }

        boolean windows = Os.isFamily( "windows" );

        Commandline cmdLine = new Commandline();
        File exec;
        if ( windows )
        {
            exec = new File( appbasedir, "bin" + brandingToken + "_w.exe" );
            if ( !exec.exists() )
            { // Was removed as of nb 6.7
                exec = new File( appbasedir, "bin\\" + brandingToken + ".exe" );
                // if jdk is 32 or 64-bit
                String jdkHome = System.getenv( "JAVA_HOME" );
                if ( jdkHome != null )
                {
                    if ( new File( jdkHome, "jre\\lib\\amd64\\jvm.cfg" ).exists() )
                    {
                        File exec64 = new File( appbasedir, "bin\\" + brandingToken + "64.exe" );
                        if ( exec64.isFile() )
                        {
                            exec = exec64;
                        }
                    }
                }
                cmdLine.addArguments( new String[] { "--console", "suppress" } );
            }
        }
        else
        {
            exec = new File( appbasedir, "bin/" + brandingToken );
        }

        cmdLine.setExecutable( exec.getAbsolutePath() );

        try
        {

            List<String> args = new ArrayList<String>();
            args.add( "--userdir" );
            args.add( netbeansUserdir.getAbsolutePath() );
            args.add( "-J-Dnetbeans.logger.console=true" );
            args.add( "-J-ea" );
            args.add( "--branding" );
            args.add( brandingToken );

            // use JAVA_HOME if set
            if ( System.getenv( "JAVA_HOME" ) != null )
            {
                args.add( "--jdkhome" );
                args.add( System.getenv( "JAVA_HOME" ) );
            }

            cmdLine.addArguments( args.toArray( new String[0] ) );
            cmdLine.addArguments( CommandLineUtils.translateCommandline( additionalArguments ) );
            cmdLine.addArguments( CommandLineUtils.translateCommandline( getDebugAdditionalArguments() ) );
            getLog().info( "Executing: " + cmdLine.toString() );
            StreamConsumer out = new StreamConsumer()
            {

                public void consumeLine( String line )
                {
                    getLog().info( line );
                }
            };
            CommandLineUtils.executeCommandLine( cmdLine, out, out );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed executing NetBeans", e );
        }
    }

    private String getDebugAdditionalArguments()
    {
       if ( "true".equals( debugAdditionalArguments ) )
        {
            return "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
        }
        return debugAdditionalArguments;
    }
}
