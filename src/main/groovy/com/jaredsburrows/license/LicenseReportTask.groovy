package com.jaredsburrows.license

import com.jaredsburrows.license.internal.pom.Developer
import com.jaredsburrows.license.internal.pom.License
import com.jaredsburrows.license.internal.pom.Project
import com.jaredsburrows.license.internal.report.HtmlReport
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.logging.LogLevel

class LicenseReportTask extends LicenseReportTaskKt {

  /**
   * Get POM information from the dependency artifacts.
   */
  @Override protected void generatePOMInfo() {
    // Iterate through all POMs in order from our custom POM configuration
    for (ResolvedArtifact pom : getProject().getConfigurations().getByName(POM_CONFIGURATION)
      .getResolvedConfiguration().getLenientConfiguration().getArtifacts()) {
      File pomFile = pom.getFile()
      Node pomText = new XmlParser().parse(pomFile)

      // License information
      String name = getName(pomText)
      String version = pomText.version?.text()
      String description = pomText.description?.text()
      List<Developer> developers = new ArrayList<>()
      if (pomText.developers) {
        developers = pomText.developers.developer?.collect { developer ->
          new Developer(name: developer?.name?.text()?.trim())
        }
      }

      String url = pomText.url?.text()
      String year = pomText.inceptionYear?.text()

      // Clean up and format
      name = name?.trim()
      version = version?.trim()
      description = description?.trim()
      url = url?.trim()
      year = year?.trim()

      // Search for licenses
      List<License> licenses = findLicenses(pomFile)
      if (!licenses) {
        getLogger().log(LogLevel.WARN, "${name} dependency does not have a license.")
        licenses = new ArrayList<>()
      }

      // Search for version
      if (!version) {
        version = findVersion(pomFile)
      }

      // Store the information that we need
      Project project = new Project(
        name: name,
        description: description,
        version: version,
        developers: developers,
        licenses: licenses,
        url: url,
        year: year,
        gav: pom.owner
      )

      projects.add(project)
    }

    // Sort POM information by name
    projects.sort { left, right -> left.getName().compareToIgnoreCase(right.getName()) }
  }

  private String findVersion(File pomFile) {
    if (!pomFile) {
      return null
    }
    Node pomText = new XmlParser().parse(pomFile)

    // If the POM is missing a name, do not record it
    String name = getName(pomText)
    if (!name) {
      getLogger().log(LogLevel.WARN, "POM file is missing a name: ${pomFile}")
      return null
    }

    if (pomText.version) {
      return pomText.version?.text()?.trim()
    }

    if (pomText.parent != null) {
      File parentPomFile = getParentPomFile(pomText)
      return findVersion(parentPomFile)
    }
    return null
  }

  @Override protected String getName(Node pomText) {
    String name = pomText.name?.text() ? pomText.name?.text() : pomText.artifactId?.text()
    return name?.trim()
  }

  @Override protected List<License> findLicenses(File pomFile) {
    if (!pomFile) {
      return null
    }
    Node pomText = new XmlParser().parse(pomFile)

    // If the POM is missing a name, do not record it
    String name = getName(pomText)
    if (!name) {
      getLogger().log(LogLevel.WARN, "POM file is missing a name: ${pomFile}")
      return null
    }

    if (ANDROID_SUPPORT_GROUP_ID == pomText.groupId?.text()) {
      return [new License(name: APACHE_LICENSE_NAME, url: APACHE_LICENSE_URL)]
    }

    // License information found
    if (pomText.licenses) {
      List<License> licenses = new ArrayList<>()
      pomText.licenses[0].license.each { license ->
        String licenseName = license.name?.text()
        String licenseUrl = license.url?.text()
        try {
          new URL(licenseUrl)
          licenseName = licenseName?.trim()?.capitalize()
          licenseUrl = licenseUrl?.trim()
          licenses.add(new License(name: licenseName, url: licenseUrl))
        } catch (Exception ignored) {
          getLogger().log(LogLevel.WARN, "${name} dependency has an invalid license URL;" +
            " skipping license")
        }
      }
      return licenses
    }
    getLogger().log(LogLevel.INFO, "Project, ${name}, has no license in POM file.")

    if (pomText.parent != null) {
      File parentPomFile = getParentPomFile(pomText)
      return findLicenses(parentPomFile)
    }
    return null
  }

  /**
   * Use Parent POM information when individual dependency license information is missing.
   */
    @Override protected File getParentPomFile(Node pomText) {
      // Get parent POM information
      String groupId = pomText?.parent?.groupId?.text()
      String artifactId = pomText?.parent?.artifactId?.text()
      String version = pomText?.parent?.version?.text()
      String dependency = "$groupId:$artifactId:$version@pom"

      // Add dependency to temporary configuration
      getProject().getConfigurations().create(TEMP_POM_CONFIGURATION)
      getProject().getConfigurations().getByName(TEMP_POM_CONFIGURATION).dependencies.add(
        getProject().getDependencies().add(TEMP_POM_CONFIGURATION, dependency)
      )
      return super.getParentPomFile(pomText)
    }

  /**
   * Generated HTML report.
   */
  @Override protected void createHtmlReport() {
    // Remove existing file
    getProject().file(htmlFile).delete()

    // Create directories and write report for file
    htmlFile.getParentFile().mkdirs()
    htmlFile.createNewFile()
    htmlFile.withOutputStream { outputStream ->
      PrintStream printStream = new PrintStream(outputStream)
      printStream.print(new HtmlReport(projects).string())
      printStream.println() // Add new line to file
    }

    // Log output directory for user
    getLogger().log(LogLevel.LIFECYCLE, "Wrote HTML report to ${getClickableFileUrl(htmlFile)}.")
  }
}
