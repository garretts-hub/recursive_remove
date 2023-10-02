package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.FilterMatchListener;
import org.junit.Assert;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;
import org.logstash.plugins.ContextImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaRecursiveRemoveFilterTest {

    @Test
    public void testJavaRecursiveRemoveFilter() {
        //We'll create three events, the first two will have bad fields that get dropped
        List<Object> valuesConfig = Arrays.asList("-", 0, "");
        List<String> excludedKeysConfig = Arrays.asList("[address][city]", "favorite_keyboard_character", "cars_owned", "[address][zip][dont_delete_me]");
        Map<String, Object> configMap = new HashMap<String, Object>() {{
            put("matching_values", valuesConfig);
            put("excluded_keys", excludedKeysConfig);
        }};
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null,null);
        RecursiveRemove recursiveRemoveFilter = new RecursiveRemove("test-id", config, context);

        // This one should drop the missing age and address.zip.extension, but leave the favorite_keyboard_character & cars_owned
        Map<String, Object> inputDataMap1 = new HashMap<String, Object>() {{
            put("name", "Jane Doe");
            put("age", "-");
            put("account_number", 654321);
            put("favorite_keyboard_character", "-");
            put("cars_owned", 0);
            put("address", (Map) new HashMap<String, Object>() {{
                put("city", "New York City");
                put("zip",(Map) new HashMap<String, Object>() {{
                    put("code", 10001);
                    put("extension",0000);
                    put("dont_delete_me", "-");
                }});
                put("state", "NY");}});
        }};
        Event e1 = new org.logstash.Event(inputDataMap1);

        // This one needs the missing account dropped, bad age dropped, but should leave the missing city and address.zip.dont_delete_me
        Map<String, Object> inputDataMap2 = new HashMap<String, Object>() {{
            put("name", "John Doe");
            put("account_number", "");
            put("age", 0.0000);
            put("cars_owned_dont_delete", 2);
            put("address", (Map) new HashMap<String, Object>() {{
                put("city", "-");
                put("zip",(Map) new HashMap<String, Object>() {{
                    put("code", 90005);
                    put("extension", 1234);
                    put("dont_delete_me", "");
                }});
                put("state", "CA");}});
        }};
        Event e2 = new org.logstash.Event(inputDataMap2);

        // This one should have all good fields - the filter should leave the  address.zip.dont_delete_me
        Map<String, Object> inputDataMap3 = new HashMap<String, Object>() {{
            put("name", "Jack Doe");
            put("account_number", 123456);
            put("age", 34);
            put("cars_owned_dont_delete", 1);
            put("address", (Map) new HashMap<String, Object>() {{
                put("city", "Houston");
                put("zip",(Map) new HashMap<String, Object>() {{
                    put("code", 7703);
                    put("extension", 2345);
                    put("dont_delete_me", "-");
                }});
                put("state", "TX");}});
        }};
        Event e3 = new org.logstash.Event(inputDataMap3);

        List<Event> eventList = Arrays.asList(e1,e2,e3);
        TestMatchListener matchListener = new TestMatchListener();
        Collection<Event> filterResults = recursiveRemoveFilter.filter(eventList, matchListener);

        int filteredEvents = filterResults.size();
        int matchedEvents = matchListener.getMatchCount();
        Assert.assertTrue("Error: expecting 3 total filtered events.", filteredEvents == 3);
        Assert.assertTrue("Error: only 2 of the 3 input events should have matched.", matchedEvents == 2);


        // Create expected maps here
        Map<String, Object> expectedDataMap1 = new HashMap<String, Object>() {{
            put("name", "Jane Doe");
            put("favorite_keyboard_character", "-");
            put("account_number", 654321);
            put("cars_owned", 0);
            put("address", (Map) new HashMap<String, Object>() {{
                put("city", "New York City");
                put("zip",(Map) new HashMap<String, Object>() {{
                    put("code", 10001);
                    put("dont_delete_me", "-");
                }});
                put("state", "NY");}});
        }};
        Map<String, Object> expectedDataMap2 = new HashMap<String, Object>() {{
            put("name", "John Doe");
            put("cars_owned_dont_delete", 2);
            put("address", (Map) new HashMap<String, Object>() {{
                put("city", "-");
                put("zip",(Map) new HashMap<String, Object>() {{
                    put("code", 90005);
                    put("extension", 1234);
                    put("dont_delete_me", "");
                }});
                put("state", "CA");}});
        }};

        List<Map<String, Object>> expectedEventMapList = Arrays.asList(expectedDataMap1,expectedDataMap2,inputDataMap3);
        Iterator<Event> resultsIterator = filterResults.iterator();
        int counter = 0;
        while (resultsIterator.hasNext()) {
            Event actualEvent = resultsIterator.next();
            Event expectedEvent = new org.logstash.Event(expectedEventMapList.get(counter));
            actualEvent.remove("@timestamp");
            expectedEvent.remove("@timestamp");
            Assert.assertEquals("Error: Event number " + Integer.toString(counter) + " does not match the expected output.", expectedEvent.toMap(), actualEvent.toMap());
            counter++;
        }
    }
}

class TestMatchListener implements FilterMatchListener {
    private AtomicInteger matchCount = new AtomicInteger(0);
    public void filterMatched(Event event) {
        matchCount.incrementAndGet();
    }
    public int getMatchCount() {
        return matchCount.get();
    }
}