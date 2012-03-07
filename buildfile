require 'rubygems'
require "bundler/setup"
require 'buildr-dependency-extensions'

# Version number for this release
VERSION_NUMBER = "1.0.#{ENV['BUILD_NUMBER'] || 'SNAPSHOT'}"
# Group identifier for your projects
GROUP = "com.springsense"
COPYRIGHT = "(C) Copyright 2012 SpringSense Trust. All rights reserved. Licensed underthe Apache License, Version 2.0. See file LICENSE."

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2"
repositories.remote << "http://repo1.maven.org/maven2"
repositories.remote << "http://192.168.0.96/~artifacts/repository"
repositories.release_to = 'sftp://artifacts:repository@192.168.0.96/home/artifacts/repository'

desc "The index-springsense project"
define "index-springsense" do
  extend PomGenerator
  extend TransitiveDependencies

  project.version = VERSION_NUMBER
  project.group = GROUP
  project.transitive_scopes = [:compile, :run, :test]

  manifest["Implementation-Vendor"] = COPYRIGHT

  NUTCH = [
    artifact('org.apache.nutch:nutch:jar:1.4'),
    artifact('org.apache.hadoop:hadoop-core:jar:0.20.2'),
    artifact('org.slf4j:slf4j-api:jar:1.6.1'),
    artifact('org.slf4j:slf4j-log4j12:jar:1.6.1'),
    artifact('commons-lang:commons-lang:jar:2.4'),
    artifact('log4j:log4j:jar:1.2.15'),
  ]
  
  DISAMBIGJ = artifacts('com.springsense:disambigj:jar:2.0.2.90')

  JUNIT4 = artifact("junit:junit:jar:4.8.2")
  HAMCREST = artifact("org.hamcrest:hamcrest-core:jar:1.2.1")
  MOCKITO = artifact("org.mockito:mockito-all:jar:1.8.5")
  COMMONS_LOGGING = artifacts('commons-logging:commons-logging:jar:1.1.1')

  compile.with DISAMBIGJ, NUTCH
  compile.using :target => "1.5"
  test.compile.with JUNIT4, HAMCREST, MOCKITO, COMMONS_LOGGING
  test.using :java_args => [ '-Xmx2g' ]

  package(:jar)
end
