package org.endoscope.core;

import org.endoscope.properties.Properties;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class EngineTest {
    @Test
    public void test_flow(){
        Engine ci = new Engine();
        ci.setEnabled(true);

        ci.push("a1");
        ci.push("a11");
        ci.pop();
        ci.pop();

        waitUtilStatsGetCollected(ci);

        ci.getStatsProcessor().process(stats -> {
            Map<String, Stat> map = stats.getMap();
            Assert.assertEquals(2, map.size());
            Assert.assertTrue(map.containsKey("a1"));
            Assert.assertTrue(map.containsKey("a11"));
            Assert.assertEquals(1, map.get("a1").getChildren().size());
            Assert.assertNotNull(map.get("a1").getChildren().get("a11"));
            return null;
        });
    }

    private void waitUtilStatsGetCollected(Engine ci) {
        for(int i=0; i< 10; i++){
            if( ci.getStatsProcessor().getQueueSize() == 0 ){
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void should_return_enabled(){
        PropertyTestUtil.withProperty(Properties.ENABLED, "true", ()->{
            Assert.assertTrue(new Engine().isEnabled());
        });
    }

    @Test
    public void should_return_disabled(){
        PropertyTestUtil.withProperty(Properties.ENABLED, "false", ()->{
            Assert.assertFalse(new Engine().isEnabled());
        });
    }
}