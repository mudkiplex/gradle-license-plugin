package com.jaredsburrows.license

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
 */
final class LicenseReportTaskSpec extends Specification {
  def project
  def assertDir

  def "setup"() {
    given:
    project = ProjectBuilder.builder().build()
    assertDir = project.file("./src/test/resources")

    // Common build.gradle
    project.apply plugin: "com.android.application"
    project.android {
      compileSdkVersion 25
      buildToolsVersion "25.0.1"

      defaultConfig {
        applicationId "com.example"
      }
    }

    assertDir.parentFile.mkdirs()
  }

  def "cleanup"() {
    assertDir.deleteDir()
  }

  def "test licenseDebugReport - build.gradle with no dependencies"() {
    given:
    project.dependencies {}

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseDebugReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "No open source libraries"
    // Nothing else
    !html.text().contains("Notice for libraries:")
  }

  def "test licenseReleaseReport - build.gradle with no dependencies"() {
    given:
    project.dependencies {}

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseReleaseReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "No open source libraries"
    // Nothing else
    !html.text().contains("Notice for libraries:")
  }

  def "test licenseDebugReport - build.gradle with no open source dependencies"() {
    given:
    project.dependencies {
      delegate.compile("com.google.firebase:firebase-core:10.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseDebugReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "No open source libraries"
    // Nothing else
    !html.text().contains("Notice for libraries:")
  }

  def "test licenseReleaseReport - build.gradle with no open source dependencies"() {
    given:
    project.dependencies {
      delegate.compile("com.google.firebase:firebase-core:10.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseReleaseReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "No open source libraries"
    // Nothing else
    !html.text().contains("Notice for libraries:")
  }

  def "test licenseDebugReport - default buildTypes"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }
    }
    project.dependencies {
      // Handles duplicates
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.compile("com.android.support:design:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseDebugReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[2]
    !html.body.pre[1]
  }

  def "test licenseReleaseReport - default buildTypes"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }
    }
    project.dependencies {
      // Handles duplicates
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.compile("com.android.support:design:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseReleaseReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[2]
    !html.body.pre[1]
  }

  def "test licenseDebugReport - default and debug buildTypes"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }
    }
    project.dependencies {
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.debugCompile("com.android.support:design:25.0.1")
      delegate.releaseCompile("com.android.support:support-annotations:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseDebugReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[2]
    !html.body.pre[1]
  }

  def "test licenseReleaseReport - default and debug buildTypes"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }
    }
    project.dependencies {
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.debugCompile("com.android.support:support-annotations:25.0.1")
      delegate.releaseCompile("com.android.support:design:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseReleaseReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[2]
    !html.body.pre[1]
  }

  def "test licenseFlavor1DebugReport - default, debug buildTypes and productFlavors"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }

      productFlavors {
        flavor1 {}
        flavor2 {}
      }
    }
    project.dependencies {
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.debugCompile("com.android.support:design:25.0.1")
      delegate.flavor1Compile("com.android.support:support-v4:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseFlavor1DebugReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.ul.li[2].text() == "Support-v4"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[3]
    !html.body.pre[1]
  }

  def "test licenseFlavor2ReleaseReport - default, debug buildTypes and productFlavors"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }

      productFlavors {
        flavor1 {}
        flavor2 {}
      }
    }
    project.dependencies {
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.releaseCompile("com.android.support:design:25.0.1")
      delegate.flavor2Compile("com.android.support:support-v4:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseFlavor2ReleaseReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.ul.li[2].text() == "Support-v4"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[3]
    !html.body.pre[1]
  }

  def "test licenseFlavor1Flavor3DebugReport - default, debug buildTypes and productFlavors dimensions"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }

      flavorDimensions "a", "b"

      productFlavors {
        flavor1 { dimension "a" }
        flavor2 { dimension "a" }
        flavor3 { dimension "b" }
        flavor4 { dimension "b" }
      }
    }
    project.dependencies {
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.debugCompile("com.android.support:design:25.0.1")
      delegate.flavor1Compile("com.android.support:support-v4:25.0.1")
      delegate.flavor3Compile("com.android.support:support-annotations:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseFlavor1Flavor3DebugReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.ul.li[2].text() == "Support-annotations"
    html.body.ul.li[3].text() == "Support-v4"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[4]
    !html.body.pre[1]
  }

  def "test licenseFlavor2Flavor4ReleaseReport - default, debug buildTypes and productFlavors dimensions"() {
    given:
    project.android {
      buildTypes {
        debug {}
        release {}
      }

      flavorDimensions "a", "b"

      productFlavors {
        flavor1 { dimension "a" }
        flavor2 { dimension "a" }
        flavor3 { dimension "b" }
        flavor4 { dimension "b" }
      }
    }
    project.dependencies {
      delegate.compile("com.android.support:appcompat-v7:25.0.1")
      delegate.releaseCompile("com.android.support:design:25.0.1")
      delegate.flavor2Compile("com.android.support:support-v4:25.0.1")
      delegate.flavor4Compile("com.android.support:support-annotations:25.0.1")
    }

    when:
    project.evaluate()
    new LicensePlugin().apply(project)

    // Change output directory for testing
    def task = project.tasks.getByName("licenseFlavor2Flavor4ReleaseReport")
    task.assetDirs = [assertDir]
    task.execute()

    then:
    def html = new XmlParser().parse(task.htmlFile)
    // Title
    html.head.title.text() == "Open source licenses"
    html.body.h3[0].text() == "Notice for libraries:"
    // Dependencies
    html.body.ul.li[0].text() == "Appcompat-v7"
    html.body.ul.li[1].text() == "Design"
    html.body.ul.li[2].text() == "Support-annotations"
    html.body.ul.li[3].text() == "Support-v4"
    html.body.pre[0].text() == "The Apache Software License, http://www.apache.org/licenses/LICENSE-2.0.txt"
    // Nothing else
    !html.body.ul.li[4]
    !html.body.pre[1]
  }
}