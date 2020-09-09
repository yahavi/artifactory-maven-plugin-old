package org.jfrog.buildinfo.utils;

import org.apache.commons.io.FilenameUtils;
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

    /**
     * Set md5 and sha1 for the input file.
     *
     * @param file      - The file to calculate the checksums
     * @param buildFile - Dependency or Artifact
     * @param logger    - The logger
     */
    public static void setChecksums(File file, BaseBuildFileBean buildFile, Log logger) {
        if (!isFile(file)) {
            return;
        }
        try {
            Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(file, "md5", "sha1");
            buildFile.setMd5(checksumsMap.get("md5"));
            buildFile.setSha1(checksumsMap.get("sha1"));
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Could not set checksum values on '" + buildFile.getLocalPath() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Get running Maven version.
     *
     * @param currentClass - The caller class
     * @return the Maven version
     */
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

    /**
     * Get the artifact name in form of 'artifactId-version-classifier.extension' or 'artifactId-version.extension'
     *
     * @param artifactId    - The artifact ID
     * @param version       - The artifact version
     * @param classifier    - The classifier
     * @param fileExtension - The extension of the file
     * @return the artifact name
     */
    public static String getArtifactName(String artifactId, String version, String classifier, String fileExtension) {
        String name = artifactId + "-" + version;
        if (StringUtils.isNotBlank(classifier)) {
            name += "-" + classifier;
        }
        return name + "." + fileExtension;
    }

    /**
     * Get the layout path in artifactory to deploy.
     *
     * @param groupId       - The group ID
     * @param artifactId    - The artifact ID
     * @param version       - The version
     * @param classifier    - The classifier
     * @param fileExtension - The extension of the file
     * @return deployment path
     */
    public static String getDeploymentPath(String groupId, String artifactId, String version, String classifier, String fileExtension) {
        return String.join("/", groupId.replace(".", "/"), artifactId, version, getArtifactName(artifactId, version, classifier, fileExtension));
    }

    /**
     * Get extension of the input file.
     *
     * @param file - The file
     * @return extension of the input file
     */
    public static String getFileExtension(File file) {
        if (file == null) {
            return StringUtils.EMPTY;
        }
        return FilenameUtils.getExtension(file.getName());
    }

    /**
     * Return true if the input File is actually a file.
     *
     * @param file - The file to check
     * @return true if the input File is actually a file
     */
    public static boolean isFile(File file) {
        return file != null && file.isFile();
    }
}
