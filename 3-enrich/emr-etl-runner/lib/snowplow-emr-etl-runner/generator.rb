# Copyright (c) 2012-2014 Snowplow Analytics Ltd. All rights reserved.
#
# This program is licensed to you under the Apache License Version 2.0,
# and you may not use this file except in compliance with the Apache License Version 2.0.
# You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the Apache License Version 2.0 is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.

# Author::    Ben Fradet (mailto:support@snowplowanalytics.com)
# Copyright:: Copyright (c) 2012-2014 Snowplow Analytics Ltd
# License::   Apache License Version 2.0

require 'contracts'
require 'open-uri'
require 'avro'

# Abstract class defining a generator behavior.
# See implementing classes: EmrClusterGenerator and PlaybookGenerator.
module Snowplow
  module EmrEtlRunner
    module Generator

      include Contracts

      # Print out the Avro record given by create_record to a file using the schema provided by
      # get_schema
      Contract ConfigHash, String, String => None
      def generate(config, version, filename)
        raw_schema = get_schema(version)
        avro_schema = Avro::Schema.parse(raw_schema)
        writer = Avro::IO::DatumWriter.new(avro_schema)
        file = File.open(filename, 'wb')
        dw = Avro::DataFile::Writer.new(file, writer, schema)
        dw << create_record(config)
        dw.close
      end

      # Get the associated Avro schema
      Contract String => String
      def get_schema(version)
        raise RuntimeError, '#get_schema needs to be defined in all generators.'
      end

      Contract ConfigHash => String
      def create_record(config)
        raise RuntimeError, '#create_record needs to be defined in all generators.'
      end

      # Download a file as a string.
      Contract String => String
      def download_as_string(url)
        open(url) { |io| data = io.read }
      end

    end
  end
end
