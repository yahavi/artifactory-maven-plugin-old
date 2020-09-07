package org.jfrog.buildinfo.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.Maven;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.api.BaseBuildFileBean;
import org.jfrog.build.api.util.FileChecksumCalculator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

/**
 * @author yahavi
 */
public class Utils {

    public static void setChecksums(File dependencyFile, BaseBuildFileBean buildFile, Log logger) {
        if (!isFile(dependencyFile)) {
            return;
        }
        try {
            Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(dependencyFile, "md5", "sha1");
            buildFile.setMd5(checksumsMap.get("md5"));
            buildFile.setSha1(checksumsMap.get("sha1"));
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Could not set checksum values on '" + buildFile.getLocalPath() + "': " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("rawtypes")
    public static String getMavenVersion(Class currentClass) {
        // Get Maven version from this class
        Properties mavenVersionProperties = new Properties();
        try (InputStream inputStream = currentClass.getClassLoader().getResourceAsStream("org/apache/maven/messages/build.properties")) {
            if (inputStream != null) {
                mavenVersionProperties.load(inputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while extracting Maven version properties from: org/apache/maven/messages/build.properties", e);
        }

        // Get Maven version from Maven core class
        if (mavenVersionProperties.isEmpty()) {
            try (InputStream inputStream = Maven.class.getClassLoader().getResourceAsStream("META-INF/maven/org.apache.maven/maven-core/pom.properties")) {
                if (inputStream != null) {
                    mavenVersionProperties.load(inputStream);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error while extracting Maven version properties from: META-INF/maven/org.apache.maven/maven-core/pom.properties", e);
            }
        }

        if (mavenVersionProperties.isEmpty()) {
            throw new RuntimeException("Could not extract Maven version: unable to find resources 'org/apache/maven/messages/build.properties' or 'META-INF/maven/org.apache.maven/maven-core/pom.properties'");
        }
        String version = mavenVersionProperties.getProperty("version");
        if (StringUtils.isBlank(version)) {
            throw new RuntimeException("Could not extract Maven version: no version property found in the resource 'org/apache/maven/messages/build.properties' or or 'META-INF/maven/org.apache.maven/maven-core/pom.properties'");
        }
        return version;
    }

    public static boolean isFile(File file) {
        return file != null && file.isFile();
    }
}
