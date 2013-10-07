**You might want to read the wiki: https://github.com/bitstrings/nbm-maven.wiki.git**

nbm-maven
=========

Fork of the codehaus nbm-maven repository --> http://svn.codehaus.org/mojo/trunk/mojo/nbm-maven

This fork mainly exists to add missing features to the webstart goal of the org.codehaus.mojo:nbm-maven-plugin.

## The versioning scheme

    {CODEHAUS_VERSION}-{BITSTRINGS_REV_VERSION}

The code base is always the codehaus nbm maven plugin of the same version.  
(I am aware of the project structure weirdness)

# nbm-maven-plugin

nbm-maven-plugin home --> http://mojo.codehaus.org/nbm-maven/nbm-maven-plugin

## Added Features

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

## Releases 

Will be available through Maven Central.

NOTICE the groupId is `org.bitstrings.maven.plugins`.

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

|Parameters|Type|Description|
|----------|----|-----------|
|signWar|`boolean`|If `true` the Web Archive (war) will be signed. <br/>**Default: `false`** <br/>**User Property: `nbm.webstart.signWar`** <br/>**Since: `3.11`**|
|generateJnlpApplicationTemplate|`boolean`|If `true`, create JNLP-INF/APPLICATION_TEMPLATE.JNLP from the jnlp. See http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/signedJNLP.html. <br/>**User Property: `nbm.webstart.generateJnlpApplicationTemplate`** <br/>**Since: `3.11`**|
|signingThreads|`integer`|The number of threads that should be used to sign the jars. If set to zero (0) it will be set to the number of processors. <br/>**Default: `0`** <br/>**User Property: `nbm.signing.threads`** <br/>**Since: `3.11`**|
|signingForce|`boolean`|If `true`, force signing of the jar file even if it doesn't seem to be out of date or already signed. <br/>**Default: `true`** <br/>**User Property: `nbm.signing.force`** <br/>**Since: `3.11`**|
|signingTsaCert|`String`|Alias in the keystore for a timestamp authority for timestamped JAR files. <br/>**User Property: `nbm.signing.tsacert`** <br/>**Since: `3.11`**|
|signingTsaUrl|`String`|URL for a timestamp authority for timestamped JAR files. <br/>**User Property: `nbm.signing.tsaurl`** <br/>**Since: `3.11`**|
|signingRetryCount|`Integer`|Number of retries before giving up if some connection problem occur while TSA signing (TSA URL). <br/>**Default: `5`** <br/>**User Property: `nbm.signing.retryCount`** <br/>**Since: `3.11`**|
|signingRemoveExistingSignatures|`boolean`|Remove any existing signature from the jar before signing. <br/>**Default: `false`** <br/>**User Property: `nbm.signing.removeExistingSignatures`** <br/>**Since: `3.11`**|
|signingMaxMemory|`String`|Set the maximum memory for the jar signer. <br/>**Default: `96m`** <br/>**User Property: `nbm.signing.maxMemory`** <br/>**Since: `3.11`**|
|webappResources|`List<Resource>`|Resources that should be included in the web archive (war). <br/>**Since: `3.11`**|

### Webapp Resources

Syntax:
    
Same as Maven build/resources.

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
    <groupId>org.bitstrings.maven.plugins</groupId>
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
