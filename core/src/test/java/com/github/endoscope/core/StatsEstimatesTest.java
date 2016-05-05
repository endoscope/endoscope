package com.github.endoscope.core;

import com.github.endoscope.properties.Properties;
import com.github.endoscope.util.JsonUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.github.endoscope.core.PropertyTestUtil.withProperty;

public class StatsEstimatesTest {
    final JsonUtil jsonUtil = new JsonUtil(true);

    //estimate stats size
    @Ignore
    @Test
    public void estimate_stats_size(){
        withProperty(Properties.MAX_STAT_COUNT, "10000000", ()->{
            System.gc();
            long before = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024);
            System.out.println("Before: " + before + " MB");
            Stats stats = new Stats();
            for( long i=0; i<1000001; i++){
                if( i % 100000 == 0 ){
                    System.gc();
                    System.out.println(i + " ~ " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024) - before) + " MB" );
                }
                stats.store(new Context("" + i, 1L));
            }

            Assert.assertEquals(stats.getMap().size(), 1000001);//make sure we didn't hit the limit
        });
    }

    //estimate json doc size
    @Ignore
    @Test
    public void estimate_json_stats_size(){
        withProperty(Properties.MAX_STAT_COUNT, "10000000", ()->{
            Stats stats = new Stats();
            for( long i=0; i<1000001; i++){
                if( i % 100000 == 0 ){
                    final long ii = i;
                    try{
                        File out = File.createTempFile("endoscope-tmp", ".json");
                        jsonUtil.toJson(stats.getMap(), out);
                        System.out.println( ii + " ~ " + (out.length()/(1024*1024)) + " MB");
                        out.delete();
                    }catch(IOException e){
                        throw new RuntimeException(e);
                    }
                }
                stats.store(new Context("" + i, 1L));
            }
            Assert.assertEquals(1000001, stats.getMap().size());//make sure we didn't hit the limit
        });
    }
}