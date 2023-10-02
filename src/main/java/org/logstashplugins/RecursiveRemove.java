package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;


import java.util.*;

// class name must match plugin name, excluding underscores and casings
@LogstashPlugin(name = "recursive_remove")
public class RecursiveRemove implements Filter {
    /* This plugin takes an array of values to search for (of any type), as well as an optional list of keys to exclude,
        then searches and removes any event keys with a value within the bad values list.
     */
    public static final PluginConfigSpec<List<Object>> VALUES_CONFIG =
            PluginConfigSpec.arraySetting("matching_values", null, false, true);

    public static final PluginConfigSpec<List<Object>> EXCLUDED_KEYS_CONFIG =
	    PluginConfigSpec.arraySetting("excluded_keys", null, false, false);

    private String id;

    private List<Object> valuesArray;

    private List<Object> excludedKeysArray;

    public RecursiveRemove(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        // Convert all numeric values into into long, and convert float into double
        List<Object> modifiedValuesArray = new ArrayList<>();
        for (Object val : config.get(VALUES_CONFIG)) {
            if (val instanceof Integer) {
                modifiedValuesArray.add(((Integer) val).doubleValue());
            } else if (val instanceof Float) {
                modifiedValuesArray.add(((Float) val).doubleValue());
            } else if (val instanceof Long) {
                modifiedValuesArray.add(((Long) val).doubleValue());
            } else {
                modifiedValuesArray.add(val);
            }
        }
        this.valuesArray = modifiedValuesArray;
	    this.excludedKeysArray = config.get(EXCLUDED_KEYS_CONFIG);
    }

    private static Boolean dropBadMapEntries(Event inputEvent, Map<String, Object> eventMap, String rootKey, List<Object> valuesArray, List<Object> excludedKeysArray) {
        String fullKeyPrefix;
        Boolean hasBadEntries = false;
        if (rootKey != null) {
            fullKeyPrefix = rootKey;
        } else {
            fullKeyPrefix = "";
        }
        for (Map.Entry<String, Object> entry : eventMap.entrySet()) {
            String key = entry.getKey();
            String fullKeyString = fullKeyPrefix + "[" + key + "]";
            Object eventValue = inputEvent.getField(fullKeyString); //normal, non-jruby classes
            //System.out.println(fullKeyString + ":" + eventValue + ":" + eventValue.getClass());
            if (excludedKeysArray.contains(key) || excludedKeysArray.contains(fullKeyString)) {
                continue;
            }
            // Convert numeric values to Doubles for matching with valuesArray
            if (eventValue.getClass() == Integer.class) {
                eventValue = ((Integer) eventValue).doubleValue();
            } else if (eventValue instanceof Float) {
                eventValue = ((Float) eventValue).doubleValue();
            } else if (eventValue instanceof Long) {
                eventValue = ((Long) eventValue).doubleValue();
            }
            if (!(eventValue instanceof Map) && (valuesArray.contains(eventValue))) {
                hasBadEntries = true;
                inputEvent.remove(fullKeyString);
            } else if (eventValue instanceof Map) {
                try {
                    @SuppressWarnings("unchecked") Map<String, Object> mappedValue = (Map<String,Object>) eventValue;
                    dropBadMapEntries(inputEvent, mappedValue, fullKeyString, valuesArray, excludedKeysArray);
                } catch (ClassCastException e) {
                    System.out.println("ClassCastException for" + eventValue);
                } finally {
                    continue;
                }
            }
        }
        return hasBadEntries;
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            Boolean hadBadEntries = dropBadMapEntries(e, e.toMap(), null, this.valuesArray, this.excludedKeysArray);
            if (hadBadEntries) {
                matchListener.filterMatched(e);
            }
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        List<PluginConfigSpec<?>> configSpecCollection = Arrays.asList(VALUES_CONFIG, EXCLUDED_KEYS_CONFIG);
        return configSpecCollection;
    }

    @Override
    public String getId() {
        return this.id;
    }
}
