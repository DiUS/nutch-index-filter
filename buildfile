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
  
  DISAMBIGJ = artifact('com.springsense:disambigj:jar:2.0.2.90')

  JUNIT4 = artifact("junit:junit:jar:4.8.2")
  HAMCREST = artifact("org.hamcrest:hamcrest-core:jar:1.2.1")
  MOCKITO = artifact("org.mockito:mockito-all:jar:1.8.5")
  COMMONS_LOGGING = artifacts('commons-logging:commons-logging:jar:1.1.1')

  run.with DISAMBIGJ
  compile.with NUTCH, DISAMBIGJ
  compile.using :target => "1.5"
  test.compile.with JUNIT4, HAMCREST, MOCKITO, COMMONS_LOGGING
  test.using :java_args => [ '-Xmx2g' ]

  package(:jar)
end

def plugin_dependencies
  
  deps = [] 
  proj = project('index-springsense')
  TransitiveDependencies.instance_eval { add_dependency(proj, deps, DISAMBIGJ, [nil, "compile", "runtime"]) }
  
  #puts "Deps:\n\t'#{deps.map(&:to_s).sort.join("'\n\t'")}"
  
  deps
end

def copy_jar_tasks(*dependencies)
  dependencies.flatten.map do | source_jar | 
    target_path = "target/index-springsense/#{Pathname.new(source_jar.to_s).basename}"
    
    file target_path => [ 'target/index-springsense', source_jar ] do
      cp source_jar.to_s, target_path
    end
  end
end

directory 'target/index-springsense'

file 'target/index-springsense/plugin.xml' => [ 'target/index-springsense', project('index-springsense').package ] do
  main_jar = File.basename(project('index-springsense').package.to_s)
  libraries = plugin_dependencies.map { | jar_path | File.basename(jar_path.to_s) }

  plugin_template = File.new('src/plugin/plugin.xml.erb').read()
  erb_template = ERB.new(plugin_template)
  
  File.open('target/index-springsense/plugin.xml', 'w') do | file | 
    file.write(erb_template.result(binding))
  end
end
  
task 'index-springsense:plugin-xml' => 'target/index-springsense/plugin.xml'

task 'index-springsense:plugin-artifact-dir' => [ 'index-springsense:plugin-xml', *copy_jar_tasks([ plugin_dependencies, project('index-springsense').package ].flatten) ]
  
plugin_artifact_target = "target/index-springsense-#{project('index-springsense').version}.tar.gz"
  
file plugin_artifact_target => 'index-springsense:plugin-artifact-dir' do
  `cd target && tar -vcf index-springsense-#{project('index-springsense').version}.tar.gz index-springsense && cd ..`
end

task 'index-springsense:plugin-artifact' => plugin_artifact_target

nutch_plugin_dir = "#{ENV['NUTCH_HOME']}/plugins/index-springsense"

directory nutch_plugin_dir

task 'index-springsense:plugin-deploy' => [ nutch_plugin_dir, 'index-springsense:plugin-artifact' ] do
  tar_file = File.join(Dir.pwd, task(plugin_artifact_target).to_s)
  puts "Will deploy '#{tar_file}' to: '#{nutch_plugin_dir}'..."

  `cd #{ENV['NUTCH_HOME']}/plugins && tar -xvzf #{tar_file} && cd -`
end

