# squatch-common

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/saasquatch/squatch-common.svg?branch=master)](https://travis-ci.org/saasquatch/squatch-common)
[![codecov](https://codecov.io/gh/saasquatch/squatch-common/branch/master/graph/badge.svg)](https://codecov.io/gh/saasquatch/squatch-common)

SaaSquatch utility libraries for Java

This library aims to provide common utilites that are missing from other common Java libraries (e.g. [commons-lang](https://commons.apache.org/proper/commons-lang/) and [Guava](https://github.com/google/guava)) with few dependencies.

## Adding squatch-common to your project

### Add the repository

Maven

```xml
<repositories>
  <repository>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
    <id>bintray-saasquatch-java-libs</id>
    <name>bintray</name>
    <url>https://dl.bintray.com/saasquatch/java-libs</url>
  </repository>
</repositories>
<pluginRepositories>
  <pluginRepository>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
    <id>bintray-saasquatch-java-libs</id>
    <name>bintray-plugins</name>
    <url>https://dl.bintray.com/saasquatch/java-libs</url>
  </pluginRepository>
</pluginRepositories>
```

Gradle

```gradle
repositories {
  maven {
    url  "https://dl.bintray.com/saasquatch/java-libs"
  }
}
```

### Add the dependency

Maven

```xml
<dependency>
  <groupId>com.saasquatch</groupId>
  <artifactId>squatch-common</artifactId>
  <version>0.1.0-alpha-1</version>
  <type>pom</type>
</dependency>
```

Gradle

```gradle
compile 'com.saasquatch:squatch-common:0.1.0-alpha-1'
```

## LICENSE

```
Copyright 2019 ReferralSaaSquatch.com Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
