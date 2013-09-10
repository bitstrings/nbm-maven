nbm-maven
=========

For of the codehaus nbm-maven repository --> http://svn.codehaus.org/mojo/trunk/mojo/nbm-maven

This fork mainly exists to add missing features to the webstart goal of the org.codehaus.mojo:nbm-maven-plugin.

(Releases will be available through Maven Central)

# nbm-maven-plugin

nbm-maven-plugin home --> http://mojo.codehaus.org/nbm-maven/nbm-maven-plugin

## Features

### Webstart

* Multithreaded jar signing (nbm.signing.threads);
* TSA signing - time based signing (nbm.signing.tsacert, nbm.signing.tsaurl, nbm.signing.retryCount);
* Remove existing signatures (nbm.signing.removeExistingSignatures);
* JNLP-INF/APPLICATION_TEMPLATE.JNLP support;
* Webapp resources;
* Able to sign war archive;
* Fix regression: As of Java 6 > release 31 (applies to Java 7), the JDK sample directory doesn't exist anymore. The nbm maven plugin uses the jnlp servlet of sample to bootstrap the application. It is hard coded. The servlet is now part of the plugin;
* Support for m2e lifecycle mapping;
* More robust jar signing to fix edge cases.

## Usage

```xml
<plugin>
    <groupId>org.bitstrings.maven.plugins</groupId>
    <artifactId>nbm-maven-plugin</artifactId>
    <version>3.11</version>
    <extensions>true</extensions>
</plugin>
```

## Goal `nbm:webstart-app`

### Added Parameters

|Parameters|Type|Since|Description|
|----------|----|-----|-----------|
|signWar|`boolean`|`3.11`|If `true` the Web Archive (war) will be signed. <br/>**Default: `false`** <br/>**User Property: `nbm.webstart.signWar`**|
|generateJnlpApplicationTemplate|`boolean`|`3.11`|If `true`, create JNLP-INF/APPLICATION_TEMPLATE.JNLP from the jnlp. See http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/signedJNLP.html. <br/>**User Property: `nbm.webstart.generateJnlpApplicationTemplate`**|
|signingThreads|`integer`|`3.11`|The number of threads that should be used to sign the jars. If set to zero (0) it will be set to the number of processors. <br/>**Default: `0`** <br/>**User Property: `nbm.signing.threads`**|
|signingForce|`boolean`|`3.11`|If `true`, force signing of the jar file even if it doesn't seem to be out of date or already signed. <br/>**Default: `true`** <br/>**User Property: `nbm.signing.force`**|
|signingTsaCert|`String`|`3.11`|Alias in the keystore for a timestamp authority for timestamped JAR files. <br/>**User Property: `nbm.signing.tsacert`**|
|signingTsaUrl|`String`|`3.11`|URL for a timestamp authority for timestamped JAR files. <br/>**User Property: `nbm.signing.tsaurl`**|
|signingRetryCount|`Integer`|`3.11`|Number of retries before giving up if some connection problem occur while TSA signing (TSA URL). <br/>**Default: `5`** <br/>**User Property: `nbm.signing.retryCount`**|
|signingRemoveExistingSignatures|`boolean`|`3.11`|Remove any existing signature from the jar before signing. <br/>**Default: `false`** <br/>**User Property: `nbm.signing.removeExistingSignatures`**|
|signingMaxMemory|`String`|`3.11`|Set the maximum memory for the jar signer. <br/>**Default: `96m`** <br/>**User Property: `nbm.signing.maxMemory`**|
|webappResources|`List<Resource>`|`3.11`|Resources that should be included in the web archive (war).|

### Webapp Resources

syntax:
    
    same as Maven build/resources

```xml
<webappResources>
    <webappResource>
        <directory>src/main/resources</directory>
    </webappResource>
</webappResources>
```

This is useful to add icons for example:

```
    src/
        main/
            resources/
                icon_128x128.png
```

Resources are placed relative to the root and may be referenced in the jnlp:

```xml
<jnlp spec="6.0+" codebase="${jnlp.codebase}" href="${master.jnlp.file.name}.jnlp">

    <information>

        <icon kind="shortcut" href="icon_128x128.png"/>

    </information>
    
    ...
```

### JNLP Application Template

See http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/signedJNLP.html

The APPLICATION_TEMPLATE.JNLP is based on the JNLP. Only the `codebase` and `href` values are replaced by `*`.

The file is placed inside the `startup.jar`.

### Example 1

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>nbm-maven-plugin</artifactId>
    <extensions>true</extensions>
    <executions>
        <execution>
            <goals>
                <goal>webstart-app</goal>
            </goals>
            <configuration>
                <masterJnlpFile>src/main/webstart/${brandingToken}.jnlp</masterJnlpFile>
                <masterJnlpFileName>${brandingToken}</masterJnlpFileName>
                <generateJnlpApplicationTemplate>true</generateJnlpApplicationTemplate>
                <additionalArguments>-J-Xms384m -J-Xmx800m -J-XX:MaxPermSize=256m -J-Djava.util.Arrays.useLegacyMergeSort=true</additionalArguments>
                <keystore>${jarsigner.keystore}</keystore>
                <keystorealias>${jarsigner.alias}</keystorealias>
                <keystorepassword>${jarsigner.storepass}</keystorepassword>
                <keystoretype>${jarsigner.storetype}</keystoretype>
                <signingRemoveExistingSignatures>true</signingRemoveExistingSignatures>
                <signingThreads>8</signingThreads>
                <webappResources>
                    <webappResource>
                        <directory>src/main/resources</directory>
                    </webappResource>
                </webappResources>
            </configuration>
        </execution>
    </executions>
</plugin>
```
