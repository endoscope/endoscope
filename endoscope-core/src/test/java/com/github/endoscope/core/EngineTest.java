package com.github.endoscope.core;

import java.util.Map;

import com.github.endoscope.properties.Properties;
import com.github.endoscope.util.PropertyTestUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EngineTest {

    public static class NoopTasksFactory implements AsyncTasksFactory {
        @Override
        public void triggerAsyncTask() {}

        @Override
        public void stopStatsProcessorThread() {}
    }


    @Test
    public void test_trigger_async_recalculation_tasks(){
        PropertyTestUtil.withProperty(Properties.ENABLED, "true", ()->{
            Engine ci = new Engine();

            ci.push("a1");
            ci.push("a11");
            ci.pop(false);
            ci.pop(false);

            waitUtilStatsGetCollected(ci);

            ci.getCurrentStats().lockReadStats(stats -> {
                Map<String, Stat> map = stats.getMap();
                assertEquals(2, map.size());
                Assert.assertTrue(map.containsKey("a1"));
                Assert.assertTrue(map.containsKey("a11"));
                assertEquals(1, map.get("a1").getChildren().size());
                Assert.assertNotNull(map.get("a1").getChildren().get("a11"));
                return null;
            });
        });
    }

    private void waitUtilStatsGetCollected(Engine ci) {
        for(int i=0; i< 10; i++){
            if( ci.getCurrentStats().getQueueSize() == 0 ){
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
    public void should_pop(){
        Engine engine = new Engine(true, null, new NoopTasksFactory() );
        engine.push("a1");
        engine.push("a11");

        //current stats are empty until we pop all elements
        assertEquals(0, engine.getCurrentStats().getQueueSize());
        engine.pop(false);
        assertEquals(0, engine.getCurrentStats().getQueueSize());
        engine.pop(false);
        assertEquals(1, engine.getCurrentStats().getQueueSize());

        engine.pop(false);//noop
        engine.popAll(false);//noop
    }

    @Test
    public void should_popAll(){
        Engine engine = new Engine(true, null, new NoopTasksFactory() );
        engine.push("a1");
        engine.push("a11");

        //current stats are empty until we pop all elements
        assertEquals(0, engine.getCurrentStats().getQueueSize());
        engine.popAll(false);
        assertEquals(1, engine.getCurrentStats().getQueueSize());

        engine.pop(false);//noop
        engine.popAll(false);//noop
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

    @Test(expected = IllegalStateException.class)
    public void should_not_allow_to_add_stats_when_disabled(){
        Engine engine = new Engine(false, null, new NoopTasksFactory() );

        engine.push("id");
    }

    @Test(expected = IllegalStateException.class)
    public void should_not_allow_to_pop_stats_when_disabled(){
        Engine engine = new Engine(false, null, new NoopTasksFactory() );

        engine.pop(false);
    }

    @Test(expected = IllegalStateException.class)
    public void should_not_allow_to_pop_all_stats_when_disabled(){
        Engine engine = new Engine(false, null, new NoopTasksFactory() );

        engine.popAll(false);
    }

    @Test(expected = IllegalStateException.class)
    public void should_not_allow_to_get_queue_when_disabled(){
        Engine engine = new Engine(false, null, new NoopTasksFactory() );

        engine.getCurrentStats();
    }

    @Test(expected = IllegalStateException.class)
    public void should_not_allow_to_get_storage_when_disabled(){
        Engine engine = new Engine(false, null, new NoopTasksFactory() );

        engine.getStorage();
    }

    /**
     * This operation cannot be secured as we need to stop any tasks any time.
     */
    @Test
    public void should_allow_to_get_tasks_factory_when_disabled(){
        NoopTasksFactory tasksFactory = new NoopTasksFactory();
        Engine engine = new Engine(false, null, tasksFactory);

        assertSame(tasksFactory, engine.getCurrentStatsAsyncTasks());
    }

    @Test
    public void should_not_monitor_operation(){
        Engine engine = new Engine(false, null, new NoopTasksFactory() );

        String result = engine.monitor("id-1", () -> "result-1" );
        assertEquals("result-1", result);
    }

    @Test
    public void should_merge_call_stats(){
        Engine engine = new Engine(true, null, new NoopTasksFactory() );

        engine.getCurrentStats().add(new Context("id", 123));
        assertEquals( 1, engine.getCurrentStats().getQueueSize());

        engine.getCurrentStats().processAllFromQueue();
        assertEquals( 0, engine.getCurrentStats().getQueueSize());

        Stat stat = engine.getCurrentStats().lockReadStats(stats -> stats.getMap().get("id") );
        assertEquals( 123, stat.getMax());
    }

    @Test
    public void should_store_call_tree(){
        Engine engine = new Engine(true, null, new NoopTasksFactory() );

        assertEquals( 0, engine.getCurrentStats().getQueueSize());

        String result = engine.monitor("id-parent", () -> {
            engine.monitor("id-child1", () -> null);
            return engine.monitor("id-child2", () -> {
                return engine.monitor("id-grand-child", () -> "grand-child-result");
            });
        });
        assertEquals("grand-child-result", result);

        assertEquals( 1, engine.getCurrentStats().getQueueSize());

        engine.getCurrentStats().processAllFromQueue();

        Stat stat = engine.getCurrentStats().lockReadStats(stats -> stats.getMap().get("id-parent") );
        assertNotNull(stat);
        assertEquals(2, stat.getChildren().size());
        assertNotNull(stat.getChild("id-child1"));
        assertNotNull(stat.getChild("id-child2"));
        assertNotNull(stat.getChild("id-child2").getChild("id-grand-child"));
    }

    @Test
    public void should_store_call_tree_ex() throws Exception {
        Engine engine = new Engine(true, null, new NoopTasksFactory() );

        assertEquals( 0, engine.getCurrentStats().getQueueSize());

        String result = engine.monitorEx("id-parent", () -> {
            engine.monitorEx("id-child1", () -> null);
            return engine.monitorEx("id-child2", () -> {
                return engine.monitorEx("id-grand-child", () -> "grand-child-result");
            });
        });
        assertEquals("grand-child-result", result);

        assertEquals( 1, engine.getCurrentStats().getQueueSize());

        engine.getCurrentStats().processAllFromQueue();

        Stat stat = engine.getCurrentStats().lockReadStats(stats -> stats.getMap().get("id-parent") );
        assertNotNull(stat);
        assertEquals(2, stat.getChildren().size());
        assertNotNull(stat.getChild("id-child1"));
        assertNotNull(stat.getChild("id-child2"));
        assertNotNull(stat.getChild("id-child2").getChild("id-grand-child"));
    }

    @Test
    public void should_handle_null_id() {
        Engine engine = new Engine(true, null, new NoopTasksFactory() );
        Map<String, Stat> map = engine.getCurrentStats().lockReadStats(stats -> stats.getMap() );

        assertTrue(map.isEmpty());

        engine.monitor(null, () -> null);
        engine.getCurrentStats().processAllFromQueue();

        assertEquals(1, map.size());
        assertNotNull(map.get("<blank>"));
    }

    @Test
    public void should_handle_blank_id() {
        Engine engine = new Engine(true, null, new NoopTasksFactory() );
        Map<String, Stat> map = engine.getCurrentStats().lockReadStats(stats -> stats.getMap() );

        assertTrue(map.isEmpty());

        engine.monitor(" ", () -> null);
        engine.getCurrentStats().processAllFromQueue();

        assertEquals(1, map.size());
        assertNotNull(map.get("<blank>"));
    }

    @Test
    public void should_not_report_errors_when_call_successful() {
        Engine engine = new Engine(true, null, new NoopTasksFactory() );
        Map<String, Stat> map = engine.getCurrentStats().lockReadStats(stats -> stats.getMap() );

        engine.monitor("id", () -> "some value");
        engine.getCurrentStats().processAllFromQueue();

        assertEquals(1, map.size());
        assertEquals(0, map.get("id").getErr());
    }

    @Test
    public void should_collect_errors_when_call_end_with_exception() {
        Engine engine = new Engine(true, null, new NoopTasksFactory() );
        Map<String, Stat> map = engine.getCurrentStats().lockReadStats(stats -> stats.getMap() );

        String exceptionMessage = "";
        try{
            engine.monitor("id", () -> { throw new RuntimeException("my error"); });
        }catch(Exception e){
            exceptionMessage = e.getMessage();
        }
        engine.getCurrentStats().processAllFromQueue();

        assertEquals("my error", exceptionMessage);
        assertEquals(1, map.size());
        assertEquals(1, map.get("id").getErr());
    }

    @Test
    public void should_collect_errors_when_call_end_with_exception_ex() {
        Engine engine = new Engine(true, null, new NoopTasksFactory() );
        Map<String, Stat> map = engine.getCurrentStats().lockReadStats(stats -> stats.getMap() );

        String exceptionMessage = "";
        try{
            engine.monitorEx("id", () -> { throw new RuntimeException("my error"); });
        }catch(Exception e){
            exceptionMessage = e.getMessage();
        }
        engine.getCurrentStats().processAllFromQueue();

        assertEquals("my error", exceptionMessage);
        assertEquals(1, map.size());
        assertEquals(1, map.get("id").getErr());
    }
}