# encoding: utf-8
require "logstash/devutils/rspec/spec_helper"
require 'logstash/plugin_mixins/ecs_compatibility_support/spec_helper'
require "logstash/filters/recursive_remove"

describe LogStash::Filters::RecursiveRemove do

  describe "Basic Recursive Remove" do
    let(:config) do <<-CONFIG
      filter {

      }
    CONFIG
    end

    #sample({"message" => "[25/05/16 09:10:38:425 BST] 00000001 SystemOut     O java.lang:type=MemoryPool,name=class storage"}) do
    #  expect(subject.get("occurred_at")).to eq("25/05/16 09:10:38:425 BST")
    #  expect(subject.get("code")).to eq("00000001")
    #   expect(subject.get("service")).to eq("SystemOut")
    #   expect(subject.get("ic")).to eq("O")
    #   expect(subject.get("svc_message")).to eq("java.lang:type=MemoryPool,name=class storage")
    #   expect(subject.get("tags")).to be_nil
    #end
  end

end