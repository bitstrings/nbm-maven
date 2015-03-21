| **NOTE:** Since the codehaus merge 3.13.2 the plugin requires JDK 1.7. |
|------------------------------------------------------------------------|

| **NOTE:** For people using NetBeans or just want to use this plugin with the **codehaus** `groupId` see [org.codehaus.mojo:nbm-maven-plugin](https://github.com/bitstrings/nbm-maven/tree/master-codehaus). |
|:-----|
| **Q. What is the difference?**<br/>A. The only differences are the plugin `groupId` and the wiring.<br/>NetBeans IDE explicitly interacts with `org.codehaus.mojo:nbm-maven-plugin`, it is in fact hardcoded. |
<br/>
**You might want to check out coming changes: [Here](https://github.com/bitstrings/nbm-maven/wiki/Things-to-Come-%28Includes-Usage%29)**

**Wiki: https://github.com/bitstrings/nbm-maven/wiki**


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
* Support JAR manifest security attributes (Permissions, Codebase, etc...);
* Support adding extra JAR manifest attributes;
* Webapp resources;
* Able to sign war archive;
* Fix regression: As of Java 6 > release 31 (applies to Java 7), the JDK sample directory doesn't exist anymore. The nbm maven plugin uses the jnlp servlet of sample to bootstrap the application. It is hard coded. The servlet is now part of the plugin;
* JNLP Servlet leak fix;
* Support for m2e lifecycle mapping;
* More robust jar signing to fix edge cases;
* Pack200.

## Releases

Will be available through Maven Central.

**NOTICE** the groupId is `org.bitstrings.maven.plugins`.

## Usage

```xml
<plugin>
    <groupId>org.bitstrings.maven.plugins</groupId>
    <artifactId>nbm-maven-plugin</artifactId>
    <version>3.14-SNAPSHOT</version>
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
|signingMaximumThreads|`integer`|The number of maximum threads that should be used to sign the jars. If set to zero (0) there will be no limit. <br/>**Default: `0`** <br/>**User Property: `nbm.signing.maxthreads`** <br/>**Since: `3.11.1-4`**|
|signingForce|`boolean`|If `true`, force signing of the jar file even if it doesn't seem to be out of date or already signed. <br/>**Default: `true`** <br/>**User Property: `nbm.signing.force`** <br/>**Since: `3.11`**|
|signingTsaCert|`String`|Alias in the keystore for a timestamp authority for timestamped JAR files. <br/>**User Property: `nbm.signing.tsacert`** <br/>**Since: `3.11`**|
|signingTsaUrl|`String`|URL for a timestamp authority for timestamped JAR files. <br/>**User Property: `nbm.signing.tsaurl`** <br/>**Since: `3.11`**|
|signingRetryCount|`Integer`|Number of retries before giving up if some connection problem occur while TSA signing (TSA URL). <br/>**Default: `5`** <br/>**User Property: `nbm.signing.retryCount`** <br/>**Since: `3.11`**|
|signingRemoveExistingSignatures|`boolean`|Remove any existing signature from the jar before signing. <br/>**Default: `false`** <br/>**User Property: `nbm.signing.removeExistingSignatures`** <br/>**Since: `3.11`**|
|signingMaxMemory|`String`|Set the maximum memory for the jar signer. <br/>**Default: `96m`** <br/>**User Property: `nbm.signing.maxMemory`** <br/>**Since: `3.11`**|
|webappResources|`List<Resource>`|Resources that should be included in the web archive (war). <br/>**Since: `3.11`**|
|autoManifestSecurityEntries|`boolean`|Automatically populate the manifest with security attributes based on the master JNLP configuration. Should be set to `true` unless you explicitly use `<jarsConfig>` and the correct manifest entries. <br/>**Default: `true`**  <br/>**Since: `3.11.1`**|
|jarsConfigs|`List<JarsConfig>`|Specific configuration for Jars.  <br/>**Since: `3.11.1`**|
|applicationName|`String`|The application name which can be used as metadata. It is also used for the `Application-Name` manifest attribute value (if `autoManifestSecurityEntries` is enabled). <br/>**Default: `The jnlp information/title or the branding token.`** <br/>**Since: `3.11.1-1`**|
|verifyJnlp|`boolean`|Verify generated webstartable content. <br/>**Default: `true`** <br/>**Since: `3.11.1-2`**|
|validateJnlpDtd|`boolean`|Online JNLP schema validation.<br/>**Default: `true`** <br/>**Since: `3.11.1-2`**|

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

### Jars Configuratons <jarsConfigs>

Structure:

```xml
<jarsConfigs>
    <jarsConfig>
        <manifestEntries>
            <permissions/> <!-- sandbox or all-permissions -->
            <codebase/> <!-- used to restrict the code base of the JAR to specific domains. You may use *. -->
            <trustedOnly/> <!-- true or false -->
            <trustedLibrary/> <!-- true or false -->
            <extraAttributes/>
        </manifestEntries>
        <removeExistingSignatures/> <!-- true or false -->
        <jarSet> <!-- not using any set will apply to all jars -->
            <includes>
                <include/> <!-- ant style pattern -->
                ...
            </includes>
            <excludes>
                <exclude/> <!-- ant style pattern -->
                ...
            </excludes>
        </jarSet>
    </jarsConfig>
</jarsConfigs>
```

### jarsConfig/jarSet

Standard Maven *includes/excludes* syntax. If no `<jarSet>` is defined then all jars all selected.

The source base directory is relative to the NetBeans application directory.

### jarsConfig/manifestEntries

|Parameters|Type|Description|
|----------|----|-----------|
|permissions|`String`|Used to verify that the permissions level requested by the RIA when it runs matches the permissions level that was set when the JAR file was created. Manifest attribute: `Permissions`. <br/>**Values: `sandbox`,`all-permissions`**<br/>**See: http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/no_redeploy.html**<br/>**Since: `3.11.1`**|
|codebase|`String`|Used to restrict the code base of the JAR to specific domains. Manifest attribute: `Codebase`. <br/>**See: http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/no_redeploy.html**<br/>**Since: `3.11.1`**|
|trustedOnly|`boolean`|Used to restrict the code base of the JAR to specific domains. Manifest attribute: `Trusted-Only`. <br/>**See: http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/mixed_code.html**<br/>**Since: `3.11.1`**|
|trustedLibrary|`boolean`|Used to restrict the code base of the JAR to specific domains. Manifest attribute: `Trusted-Library`. <br/>**See: http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/mixed_code.html**<br/>**Since: `3.11.1`**|
|applicationName|`String`|The application name. Manifest attribute: `Application-Name`. <br/>**Since: `3.11.1-1`**|
|applicationLibraryAllowableCodebase|`String`|Identifies the locations where your signed RIA is expected to be found. Manifest attribute: `Application-Library-Allowable-Codebase`. <br/>**Since: `3.11.1-1`**|
|callerAllowableCodebase|`String`|Identify the domains from which JavaScript code can make calls to your RIA. Manifest attribute: `Caller-Allowable-Codebase`. <br/>**Since: `3.11.1-1`**|
|extraAttributes|`Map`|Extra manifest main attributes.<br/>**Since: `3.11.1`**|

### jarsConfig/manifestEntries/extraAttributes

Structure:

```xml
<extraAttributes>
    <name1>value1</name1> <!-- attribute name/value -->
    <name2>value2</name2> <!-- attribute name/value -->
    ...
</extraAttributes>
```

### JNLP Application Template

See http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/signedJNLP.html

The APPLICATION_TEMPLATE.JNLP is based on the JNLP. Only the `codebase` and `href` values are replaced by `*`.

The file is placed inside the `startup.jar`.

### Examples

 * **Use 8 signing threads, remove existing signatures and include resources into war.**

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

 * **Remove existing signatures and make an exception for all JARs containing "commons" in its name (set Permissions to sandbox, the associated JNLP will be correclty configured).**

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
                <jarsConfigs>
                    <jarsConfig>
                        <jarSet>
                            <includes>
                                <include>**/${brandingToken}/**/*commons*.jar</include>
                            </includes>
                        </jarSet>
                        <manifestEntries>
                            <permissions>sandbox</permissions>
                        </manifestEntries>
                    </jarsConfig>
                <jarsConfigs>
            </configuration>
        </execution>
    </executions>
</plugin>

```
 * **Use Pack200 and set the application name.**

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
                <keystore>${jarsigner.keystore}</keystore>
                <keystorealias>${jarsigner.alias}</keystorealias>
                <keystorepassword>${jarsigner.storepass}</keystorepassword>
                <keystoretype>${jarsigner.storetype}</keystoretype>
                <signingRemoveExistingSignatures>true</signingRemoveExistingSignatures>
                <applicationName>The Application Name</applicationName>
                <pack200>true</pack200>
            </configuration>
        </execution>
    </executions>
</plugin>
```
