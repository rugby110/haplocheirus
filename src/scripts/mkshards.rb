#!/usr/bin/env ruby
#
# Copyright 2010 Twitter, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

$:.push(File.dirname($0))
require 'optparse'
require 'yaml'

options = {
  :config_filename => ENV['HOME'] + "/.shards.yml",
  :count => 500,
}

parser = OptionParser.new do |opts|
  opts.banner = "Usage: #{$0} [options]"
  opts.separator "Example: #{$0} -f shards.yml"

  opts.on("-f", "--config=FILENAME", "load shard database config (default: #{options[:config_filename]})") do |filename|
    options[:config_filename] = filename
  end
  opts.on("-n", "--count=N", "create N bins (default: #{options[:count]})") do |count|
    options[:count] = count.to_i
  end
end

parser.parse!(ARGV)

config = begin
  YAML.load_file(options[:config_filename])
rescue => e
  puts "Exception while reading config file: #{e}"
  {}
end

app_host, app_port = (config['app_host'] || 'localhost').split(':')
app_port ||= 7668

namespace = config['namespace'] || nil
db_trees = Array(config['databases'] || 'localhost')

gizzmo = lambda do |cmd|
  `gizzmo --host=#{app_host} --port=#{app_port} #{cmd}`
end


print "Creating bins"
STDOUT.flush
options[:count].times do |i|
  table_name = [ namespace, "haplo_%04d" % i ].compact.join("_")
  hosts = Array(db_trees[i % db_trees.size])
  lower_bound = (1 << 60) / options[:count] * i

  gizzmo.call "create com.twitter.gizzard.shards.ReplicatingShard localhost/#{table_name}_replicating"

  distinct = 1
  hosts.each do |host|
    host, weight = host.split('/')
    weight ||= 4
    gizzmo.call "create com.twitter.haplocheirus.RedisShard #{host}/#{table_name}_#{distinct}"
    gizzmo.call "addlink localhost/#{table_name}_replicating #{host}/#{table_name}_#{distinct} #{weight}"
    distinct += 1
  end

  gizzmo.call "addforwarding -- 0 #{lower_bound} localhost/#{table_name}_replicating"

  print "."
  print "#{i+1}" if (i + 1) % 100 == 0
  STDOUT.flush
end
puts "Done."
