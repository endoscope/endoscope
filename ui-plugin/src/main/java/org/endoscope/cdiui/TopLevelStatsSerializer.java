package org.endoscope.cdiui;

import org.endoscope.core.Stat;

import java.util.Map;

import static java.util.Collections.EMPTY_MAP;

public class TopLevelStatsSerializer {
    public String serialize(Map<String, Stat> map) {
        StringBuilder sb = new StringBuilder("{");

        map.forEach((id, stat) -> {
            sb.append("\"").append(id).append("\":{");
            sb.append("\"hits\":"  ).append(stat.getHits()).append(",");
            sb.append("\"max\":"    ).append(stat.getMax()  ).append(",");
            sb.append("\"min\":"    ).append(stat.getMin()  ).append(",");
            sb.append("\"avg\":"    ).append(stat.getAvg()  ).append(",");
            sb.append("\"ah10\":").append(stat.getAh10()).append(",");
            sb.append("\"children\":").append(stat.getChildren() == null ? null : EMPTY_MAP);//no comma here
            sb.append("},");
        });

        if( !map.isEmpty() ){
            sb.deleteCharAt(sb.length()-1);//remove last comma
        }
        sb.append("}");
        return sb.toString();
    }
}
