# `recursive_remove` - a Logstash Java-based Filter Plugin

`recursive_remove` is a Java-based, filter plugin for [Logstash](https://github.com/elastic/logstash). This plugin allows you to arbitrarily remove _any_ event fields whose values match a specified blacklist, specified in the `matching_values` array argument. The plugin can search nested fields and does not require you to specify the name of the fields to remove, differentiating it from the existing `prune` filter plugin's `blacklist` option.

## Usage & Arguments
- **`matching_values`**: (Required) An array input of values to blacklist. Can consist of mixed types. All numeric values in this array and in the event will be converted to a Double during comparison, so a 0 integer in this argument will remove any event fields possessing a 0.00 float or double value.
- **`excluded_keys`**: (Optional) An array input of string fieldnames to bypass. This _can_ include nested fields, written in the standard nested Ruby syntax (`"[root][subfield][subsubfield]"`). These fields, if present in the event, will **not** be evaluated against the value blacklist specified in `matching_values`.

```
filter {
  recursive_remove {
    matching_values => ["-", 0]                     # <<< Remove any fields with a dash string or a zero numeric value
    excluded_keys => ["inventory", "[name][common]"]# <<<<  EXCEPT...if their name is "inventory" or "name.common"
  }
}
```

## Full Usage Example
The configuration below will generate three events, apply the `recursive_remove` filter, and generated the expected output.
```
input {
  generator {
    lines => [
      '{"name":"apple", "price":0, "inventory":0, "store":"-"}',
      '{"name":"pear", "price":{"ripe":1.99, "rotten":0}, "inventory":8, "store":"Walmart"}',
      '{"name":{"scientific":"musa acuminata", "common":"-"}, "price":{"ripe":0.99, "rotten":"-"}, "inventory":4, "store":"Kroger"}'
    ]
    count => 1
  }
}

filter {
  json {
    source => "message"
    target => ""
    remove_field => "message"
  }
  recursive_remove {
    matching_values => ["-", 0]
    excluded_keys => ["inventory", "[name][common]"]
  }
  mutate {
    remove_field => ["message", "event", "host", "@version", "@timestamp"]
  }
}

output {
  stdout {codec => rubydebug }
}
```

#### Expected Output 
```
{
        "price" => {
        "ripe" => 1.99
    },
         "name" => "pear",
    "inventory" => 8,
        "store" => "Walmart"
}
{
        "price" => {
        "ripe" => 0.99
    },
         "name" => {
        "scientific" => "musa acuminata",
            "common" => "-"
    },
    "inventory" => 4,
        "store" => "Kroger"
}
{
         "name" => "apple",
    "inventory" => 0
}
```

## Notes & Acknowledgements
This plugin is based off Elastic's endorsed Java Filter Plugin template found [here](https://github.com/logstash-plugins/logstash-filter-java_filter_example). 

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

The documentation for Logstash Java plugins is available [here](https://www.elastic.co/guide/en/logstash/current/contributing-java-plugin.html).
