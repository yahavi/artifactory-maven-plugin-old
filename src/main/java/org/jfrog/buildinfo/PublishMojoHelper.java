package org.jfrog.buildinfo;

import groovy.lang.Delegate;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Helper merging all mojo properties.
 *
 * @author yahavi
 */
public class PublishMojoHelper {
    @Delegate
    private PublishMojo mojo;

    private Properties systemProperties;

    /**
     * Mapping of mojo parameters of type {@link Config.DelegatesToPrefixPropertyHandler}: name => value.
     */
    private Map<String, Config.DelegatesToPrefixPropertyHandler> prefixPropertyHandlers;

    //
//    /**
//     * Mapping of types printed by {@link #printConfigurations()}: class => description.
//     */
//    private final static Map<Class<?>, String> TYPE_DESCRIPTORS = [(Boolean ):'true/false',
//            (boolean ):'true/false',
//            (Integer ):'N',
//            (Long    ):'N',
//            (File    ):'path/to/file',
//            (String  ):' .. '].
//
//    asImmutable();
    public PublishMojoHelper(PublishMojo mojo) {
        this.mojo = mojo;
        final String systemProperty = System.getProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        try {
            this.systemProperties = StringUtils.isEmpty(systemProperty) ? readProperties("") : readPropertiesFromFile(new File(systemProperty));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //        prefixPropertyHandlers = ((Map) mojo.class.declaredFields.
//                findAll {
//            Field f ->Config.DelegatesToPrefixPropertyHandler.isAssignableFrom(f.type)
//        }.
//        inject([:] ){
//            Map m, Field f -> m[f.name] = mojo. "${ f.name }";
//            m
//        }).
//        asImmutable()
    }


    /**
     * Retrieves current Maven version.
     */
    String mavenVersion() throws IOException {
        String resourceLocation = "META-INF/maven/org.apache.maven/maven-core/pom.properties";
        try (InputStream resourceStream = getClass().getResourceAsStream(resourceLocation)) {
            if (resourceStream == null) {
                throw new RuntimeException("Failed to load " + resourceLocation);
            }
            Properties properties = new Properties();
            properties.load(resourceStream);
            String version = properties.getProperty("version");
            return StringUtils.isNotBlank(version) ? version : "unknown";
        }
    }


    /**
     * Prints out all possible Mojo <configuration> settings.
     */
//    void printConfigurations() {
//        final Map<String, Object> objectsMap = ['' :this, artifactory :artifactory ]+prefixPropertyHandlers
//        final List<String> lines = ['Possible <configuration> values:' ]+objectsMap.collect {
//            String objectName, Object object ->
//                    objectName ?["<$objectName>", objectConfigurations(object).collect {
//                "  $it"
//            },"</$objectName>" ] :
//            objectConfigurations(object)
//        }.flatten()
//
//        log.debug(lines.join('\n'))
//    }


    /**
     * Retrieves a list of all object settings.
     */
//    private List<String> objectConfigurations(Object object) {
//        object.class.methods.findAll {
//            Method m ->(m.name.length() > 3) &&
//                    (m.name.startsWith('set')) &&
//                    (m.parameterTypes.length == 1) &&
//                    TYPE_DESCRIPTORS.keySet().any {
//                it.isAssignableFrom(m.parameterTypes.first())
//            }
//        }.
//        collect {
//            Method m ->
//            final tag ="${ m.name.charAt( 3 ).toLowerCase()}${ m.name.substring( 4 )}"
//            "<$tag>${ TYPE_DESCRIPTORS[ m.parameterTypes.first()] }</$tag>"
//        }.
//        sort()
//    }


    /**
     * Merges *.properties files with <configuration> values and writes a new *.properties file to be picked up later
     * by the original Maven listener.
     */
//    Properties createPropertiesFile() {
//        final mergedProperties =mergeProperties()
//        assert mergedProperties
//
//        final propertiesFile =artifactory.propertiesFile ? new File(mojo.project.basedir, artifactory.propertiesFile) :
//                File.createTempFile('buildInfo', '.properties')
//
//        mergedProperties[BuildInfoConfigProperties.PROP_PROPS_FILE] = propertiesFile.canonicalPath
//        propertiesFile.withWriter {
//            Writer w ->mergedProperties.store(w, 'Build Info Properties')
//        }
//        if (!artifactory.propertiesFile) {
//            propertiesFile.deleteOnExit()
//        }
//
//        log.info("Merged properties file:${ propertiesFile.canonicalPath } created")
//        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, propertiesFile.canonicalPath)
//
//        mergedProperties
//    }


    /**
     * Merges system-provided properties with POM properties and class fields.
     */
//    @SuppressWarnings(['GroovyAccessibility'])
//    @Requires({prefixPropertyHandlers && artifactory.delegate.root.props && buildInfo.buildTimestamp && buildInfo.buildName})
//    @Ensures({result != null})
//    private Properties mergeProperties() {
//        assert prefixPropertyHandlers.values().each {
//            assert it.delegate ?.props && it.delegate.props.is(artifactory.delegate.root.props)
//        }
//
//        final mergedProperties =new Properties()
//        final deployProperties =([(BuildInfoFields.BUILD_TIMESTAMP) :buildInfo.buildTimestamp,
//                (BuildInfoFields.BUILD_NAME) :buildInfo.buildName,
//                (BuildInfoFields.BUILD_NUMBER) :buildInfo.buildNumber ]+
//                deployProperties ).collectEntries {
//            String key, String value ->["${ ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX }${ key }".toString(), value ]
//        }
//
//        addProperties((Map<String, String>) readPropertiesFromFile(mojo.propertiesFile) + readProperties(mojo.properties),
//                mergedProperties)
//
//        addProperties(artifactory.delegate.root.props,
//                mergedProperties)
//
//        addProperties((Map<String, String>) deployProperties,
//                mergedProperties)
//
//        (Properties) mergedProperties.collectEntries {
//            String key, String value ->
//
//            final valueUpdated =(value != null) ? updateValue(value) : null
//            (valueUpdated != null) ? [(key) :valueUpdated ] : [ : ]
//        }
//    }


    /**
     * Adds all {@code propertiesFrom} to {@code propertiesTo} considering {@link #systemProperties}.
     */
//    @SuppressWarnings(['GroovyAssignmentToMethodParameter'])
//    @Requires({(propertiesFrom != null) && (propertiesTo != null) && (systemProperties != null)})
//    private void addProperties(Map<String, String> propertiesFrom, Properties propertiesTo) {
//        propertiesFrom.each {
//            String propertyName, String propertyValue ->
//                    propertyName = propertyName.toString() // Possible GString => String
//            final newValue =((propertyValue == null) || (systemProperties[propertyName] && (!pomPropertiesPriority))) ?
//                    systemProperties[propertyName] :
//                    propertyValue
//            if (newValue != null) {
//                propertiesTo[propertyName] = newValue
//            }
//        }
//    }


    /**
     * Reads {@link Properties} from the {@link File} specified.
     */
//    @Ensures({result != null})
    private Properties readPropertiesFromFile(File propertiesFile) throws IOException {
        if (propertiesFile == null) {
            throw new RuntimeException("Properties file [$propertiesFile.canonicalPath] is not available");
        }
        return readProperties(FileUtils.readFileToString(propertiesFile, StandardCharsets.UTF_8));
    }


    /**
     * Reads {@link Properties} from the {@link String} specified.
     */
    private Properties readProperties(String propertiesContent) throws IOException {
        Properties p = new Properties();
        if (propertiesContent != null) {
            p.load(new StringReader(propertiesContent));
        }
        return p;
    }


    /**
     * Updates all "{{var1|var2|var3}}" entries in the value specified to their corresponding environment variables
     * or system properties. Last variable is the fallback (default) value if wrapped in double quotes.
     * See PublishMojoHelperSpec.
     */
//    String updateValue(String value) {
//        if (!value ?.with {
//            contains('{{') && contains('}}')
//        }){
//            return value ?.trim()
//        }
//
//        final isQuoted    ={
//            String s ->s ?.with {
//                startsWith('"') && endsWith('"')
//            }
//        }
//        final unquote     ={
//            String s ->s.substring(1, s.size() - 1)
//        }
//        boolean defaultUsed = false
//        final result      =value.trim().replaceAll( /\{\{
//            ([ ^
//        }]*)\}\}/)
//
//    {
//
//        final expressions  =((String) it[1]).tokenize('|') *.trim().grep()
//
//        if (!expressions) {
//            return null
//        }
//
//        final lastValue    =expressions[-1]
//        final defaultValue =isQuoted(lastValue) ? unquote(lastValue) : null
//        defaultUsed = (defaultValue != null) || defaultUsed
//        final variables    =(defaultValue != null) ? ((expressions.size() > 1) ? expressions[0 .. - 2 ] : [] ) :
//        expressions
//
//        variables.collect {
//        System.getenv(it) ?:System.getProperty(it)
//    }.grep()[0] ?:defaultValue
//    }
//
//        (result !='null')?result.replace('null',''):
//            (defaultUsed      )?'':
//            null
//}
}
