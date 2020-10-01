# Z3 Native Libs

Add and use z3 native libraries in your application capsule in a cross
platform way without any manual steps needed on your part.  You won't
have to manually set platform-dependent DYLD\_LIBRARY\_PATH,
LD\_LIBRARY\_PATH, or PATH environment variables when you run your
application in a capsule.  The capsule will unpack the z3 native libs
into its app cache directory and the SharedLibraryPathCapsule caplet
will set whichever environment variable your platform needs to find
these native libs.

## How to build z3-native-libs

All you have to do is to say `mvn clean install` and the pom will do
the rest.  It will tell Maven to:

1. Download the most recently released z3 binaries for osx, ubuntu,
   and win platforms and unpack them in the target directory

2. Build the SharedLibraryPathCapsule caplet class which sets the
   appropriate environment variable needed to find the native libs

3. Add the native libs and the SharedLibraryPathCapsule caplet class
   to the z3-native-libs jar

4. Install both the z3-native-libs jar and the com.microsoft.z3 jar in
   the local Maven cache so other poms can find them.

## How to use z3-native-libs

Add the following two dependencies to your pom (one to let your app
call the com.microsoft.z3 Java API, the other to add the native libs
to your capsule):

```xml
        <dependency>
            <groupId>com.ge.verdict</groupId>
            <artifactId>z3-native-libs</artifactId>
            <classifier>api</classifier>
        </dependency>
        <!-- Dependencies needed only by tests or capsule jar -->
        <dependency>
            <groupId>com.ge.verdict</groupId>
            <artifactId>z3-native-libs</artifactId>
            <scope>provided</scope>
        </dependency>
```

Call maven-dependency-plugin in your pom to unpack the native libs in
z3-native-libs into your target/native-libs directory:

```xml
            <!-- Unpack z3-native-libs -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>unpack-native-libs</id>
                        <phase>process-test-resources</phase>
                        <goals>
                            <goal>unpack-dependencies</goal>
                        </goals>
                        <configuration>
                            <excludeClassifiers>api</excludeClassifiers>
                            <excludes>META-INF/</excludes>
                            <includeArtifactIds>z3-native-libs</includeArtifactIds>
                            <outputDirectory>${project.build.directory}/native-libs</outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

Call maven-surefire-plugin in your pom to run unit tests with these
native libs:

```xml
            <!-- Run unit tests with native libs -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <environmentVariables>
                        <DYLD_LIBRARY_PATH>${project.build.directory}/native-libs</DYLD_LIBRARY_PATH>
                        <LD_LIBRARY_PATH>${project.build.directory}/native-libs</LD_LIBRARY_PATH>
                        <PATH>${project.build.directory}/native-libs${path.separator}${env.PATH}</PATH>
                    </environmentVariables>
                </configuration>
            </plugin>
```

Add the native libs to your capsule with these `<caplet>` and
`<fileSets>` elements in your capsule-maven-plugin configuration:

```xml
            <!-- Add native libs to capsule -->
            <plugin>
                <groupId>com.github.chrisdchristo</groupId>
                <artifactId>capsule-maven-plugin</artifactId>
                <configuration>
                    <caplets>SharedLibraryPathCapsule</caplets>
                    <fileSets>
                        <fileSet>
                            <directory>${project.build.directory}/native-libs</directory>
                            <includes>
                                <include>*</include>
                            </includes>
                        </fileSet>
                    </fileSets>
```

That's it - now your application will be all set up to use the z3
native libs.
