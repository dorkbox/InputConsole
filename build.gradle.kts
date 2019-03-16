/*
 * Copyright 2018 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.file.Paths
import java.time.Instant

buildscript {
    // load properties from custom location
    def propsFile = Paths.get("${projectDir}/../../gradle.properties").normalize().toFile()
    if (propsFile.canRead()) {
        println("Loading custom property data from: ${propsFile}")

        def props = new Properties()
        propsFile.withInputStream {props.load(it)}
        props.each {key, val -> project.ext.set(key, val)}
    }
    else {
        ext.sonatypeUsername = ""
        ext.sonatypePassword = ""
    }

    // for plugin publishing and license sources
    repositories {
        maven {url "https://plugins.gradle.org/m2/"}
    }
    dependencies {
        // this is the only way to also get the source code for IDE auto-complete
        classpath "gradle.plugin.com.dorkbox:Licensing:1.2.2"
        classpath "gradle.plugin.com.dorkbox:Licensing:1.2.2:sources"
    }
}

plugins {
    id 'java'
    id 'maven-publish'
    id 'signing'

    // close and release on sonatype
    id 'io.codearte.nexus-staging' version '0.11.0'

    id "com.dorkbox.CrossCompile" version "1.0.1"
    id "com.dorkbox.VersionUpdate" version "1.2"

    // setup checking for the latest version of a plugin or dependency (and updating the gradle build)
    id "se.patrikerdes.use-latest-versions" version "0.2.3"
    id 'com.github.ben-manes.versions' version '0.16.0'
}

// this is the only way to also get the source code for IDE auto-complete
apply plugin: "com.dorkbox.Licensing"

// give us access to api/implementation differences for building java libraries
apply plugin: 'java-library'



project.description = 'Unbuffered input and ANSI output support for Linux, MacOS, or Windows for Java 6+'
project.group = 'com.dorkbox'
project.version = '3.6'

project.ext.name = 'Console'
project.ext.url = 'https://git.dorkbox.com/dorkbox/Console'


sourceCompatibility = JavaVersion.VERSION_1_6
targetCompatibility = JavaVersion.VERSION_1_6


licensing {
    license(License.APACHE_2) {
        author 'dorkbox, llc'
        url project.ext.url
        note project.description
    }

    license('Dorkbox Utils', License.APACHE_2) {
        author 'dorkbox, llc'
        url 'https://git.dorkbox.com/dorkbox/Utilities'
    }

    license('JAnsi', License.APACHE_2) {
        copyright 2009
        author 'Progress Software Corporation'
        author 'Joris Kuipers'
        author 'Jason Dillon'
        author 'Hiram Chirino'
        url 'https://github.com/fusesource/jansi'
    }

    license('JLine2', License.BSD_2) {
        copyright 2012
        author 'Marc Prud\'hommeaux <mwp1@cornell.edu>'
        author 'Daniel Doubrovkine'
        author 'Torbjorn Granlund'
        author 'David MacKenzie'
        url 'https://github.com/jline/jline2'
    }

    license('JNA', License.APACHE_2) {
        copyright 2011
        author 'Timothy Wall'
        url 'https://github.com/twall/jna'
    }

    license('SLF4J', License.MIT) {
        copyright 2008
        author 'QOS.ch'
        url 'http://www.slf4j.org'
    }
}

sourceSets {
    main {
        java {
            setSrcDirs Collections.singletonList('src')
        }
    }
}


repositories {
    mavenLocal() // this must be first!
    jcenter()
}


dependencies {
    // utilities dependencies compile only
    compileOnly(project('Utilities'))

    // our main dependencies are ALSO the same as the limited utilities (they are not automatically pulled in from other sourceSets)
    // needed by the utilities (custom since we don't want to include everything). IntelliJ includes everything, but our builds do not

    api 'net.java.dev.jna:jna:4.5.2'
    api 'net.java.dev.jna:jna-platform:4.5.2'

    api 'org.slf4j:slf4j-api:1.7.25'
}

///////////////////////////////
//////    Task defaults
///////////////////////////////
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}

tasks.withType(Jar) {
    duplicatesStrategy DuplicatesStrategy.FAIL

    manifest {
        attributes['Implementation-Version'] = version
        attributes['Build-Date'] = Instant.now().toString()
        attributes['Automatic-Module-Name'] = project.ext.name.toString()
    }
}

///////////////////////////////
//////    UTILITIES COMPILE (for inclusion into jars)
///////////////////////////////
static String[] utilFiles(String... fileNames) {
    def fileList = [] as ArrayList

    for (name in fileNames) {
        def fixed = name.replace('.', '/') + '.java'
        fileList.add(fixed)
    }

    return fileList
}

task compileUtils(type: JavaCompile) {
    // we don't want the default include of **/*.java
    getIncludes().clear()

    source = Collections.singletonList('../Utilities/src')

    include utilFiles('dorkbox.util.OS',
                      'dorkbox.util.OSType',
                      'dorkbox.util.Property',
                      'dorkbox.util.FastThreadLocal',
                      'dorkbox.util.bytes.ByteBuffer2',

                      'dorkbox.util.jna.JnaHelper',
                      'dorkbox.util.jna.windows.Kernel32',
                      'dorkbox.util.jna.windows.structs.CONSOLE_SCREEN_BUFFER_INFO',
                      'dorkbox.util.jna.windows.structs.INPUT_RECORD',
                      'dorkbox.util.jna.windows.structs.SMALL_RECT',
                      'dorkbox.util.jna.windows.structs.COORD',
                      'dorkbox.util.jna.windows.structs.MOUSE_EVENT_RECORD',
                      'dorkbox.util.jna.windows.structs.KEY_EVENT_RECORD',
                      'dorkbox.util.jna.windows.structs.CharUnion',

                      'dorkbox.util.jna.linux.CLibraryPosix',
                      'dorkbox.util.jna.linux.structs.Termios')

    classpath = sourceSets.main.compileClasspath
    destinationDir = file("$rootDir/build/classes_utilities")
}

jar {
    dependsOn compileUtils

    // include applicable class files from subset of Utilities project
    from compileUtils.destinationDir
}

/////////////////////////////
////    Maven Publishing + Release
/////////////////////////////
task sourceJar(type: Jar) {
    description = "Creates a JAR that contains the source code."

    from sourceSets.main.java

    classifier = "sources"
}

task javaDocJar(type: Jar) {
    description = "Creates a JAR that contains the javadocs."

    classifier = "javadoc"
}

// for testing, we don't publish to maven central, but only to local maven
publishing {
    publications {
        maven(MavenPublication) {
            from components.java

            artifact(javaDocJar)
            artifact(sourceJar)

            groupId project.group
            artifactId project.ext.name
            version project.version

            pom {
                name = project.ext.name
                url = project.ext.url
                description = project.description

                issueManagement {
                    url = "${project.ext.url}/issues".toString()
                    system = 'Gitea Issues'
                }

                organization {
                    name = 'dorkbox, llc'
                    url = 'https://dorkbox.com'
                }

                developers {
                    developer {
                        name = 'dorkbox, llc'
                        email = 'email@dorkbox.com'
                    }
                }

                scm {
                    url = project.ext.url
                    connection = "scm:${project.ext.url}.git".toString()
                }
            }
        }
    }

    repositories {
        maven {
            url "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            credentials {
                username sonatypeUsername
                password sonatypePassword
            }
        }
    }
}

nexusStaging {
    username sonatypeUsername
    password sonatypePassword
}

signing {
    sign publishing.publications.maven
}

// output the release URL in the console
releaseRepository.doLast {
    def URL = 'https://oss.sonatype.org/content/repositories/releases/'
    def projectName = project.group.toString().replaceAll('\\.', '/')
    def name = project.ext.name
    def version = project.version

    println("Maven URL: ${URL}${projectName}/${name}/${version}/")
}

// we don't use maven with the plugin (it's uploaded separately to gradle plugins)
tasks.withType(PublishToMavenRepository) {
    onlyIf {
        repository == publishing.repositories.maven && publication == publishing.publications.maven
    }
}
tasks.withType(PublishToMavenLocal) {
    onlyIf {
        publication == publishing.publications.maven
    }
}
