package com.github.endoscope.core;

import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"hits", "err", "max", "min", "avg", "children"})
public class Stat {
    private long hits = 0;
    private long err = 0;
    private long max = -1;//-1 means it's not set
    private long min = 0;
    private double avg = 0;

    private Map<String, Stat> children;

    public Stat() {
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getErr() {
        return err;
    }

    public void setErr(long err) {
        this.err = err;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getAvg() {
        return Math.round(avg);
    }

    public void setAvg(long avg) {
        this.avg = avg;
    }

    public Map<String, Stat> getChildren() {
        return children;
    }

    public void setChildren(Map<String, Stat> children) {
        this.children = children;
    }

    public void ensureChildrenMap() {
        if (children == null) {
            children = new HashMap<>();
        }
    }

    @Transient
    public Stat getChild(String id) {
        ensureChildrenMap();
        return children.get(id);
    }

    @Transient
    public Stat createChild(String id) {
        ensureChildrenMap();
        Stat child = new Stat();
        children.put(id, child);
        return child;
    }

    public void update(long time) {
        if (time < 0) return;
        if (max < 0) {
            avg = max = min = time;
        } else {
            max = Math.max(max, time);
            min = Math.min(min, time);
            avg = (avg * hits + time) / (hits + 1);
        }
        hits++;
    }

    public void updateErr(boolean err) {
        if (err) {
            this.err++;
        }
    }

    /**
     * Warning:
     * If you merge to empty stats you will most likely skip min value - it will stay 0.
     * You may need to handle it manually.
     * <p>
     * Alternatively consider using {@link #deepCopy()}
     */
    @Transient
    public void merge(Stat inc) {
        merge(inc, true);
    }

    /**
     * Warning:
     * If you merge to empty stats you will most likely skip min value - it will stay 0.
     * You may need to handle it manually.
     * <p>
     * Alternatively consider using {@link #deepCopy(boolean)}
     */
    @Transient
    public void merge(Stat inc, boolean withChildren) {
        max = Math.max(max, inc.max);
        min = Math.min(min, inc.min);
        err += inc.err;

        if (hits + inc.hits > 0) {
            avg = (avg * hits + inc.avg * inc.hits) / (hits + inc.hits);
            hits += inc.hits;
        }

        if (withChildren) {
            mergeChildren(inc);
        } else {
            //we want to mark that there are children
            if (inc.getChildren() != null) {
                ensureChildrenMap();
            }
        }
    }

    @Transient
    private void mergeChildren(Stat s2) {
        if (s2.getChildren() == null) {
            return;
        }
        ensureChildrenMap();

        s2.children.forEach((k2, v2) -> {
            Stat v1 = children.get(k2);
            if (v1 == null) {
                children.put(k2, v2);
            } else {
                v1.merge(v2);
            }
        });
    }

    @Transient
    public Stat deepCopy() {
        return deepCopy(true);
    }

    @Transient
    public Stat deepCopy(boolean withChildren) {
        Stat s = new Stat();
        s.merge(this, withChildren);
        s.setMin(min);
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stat)) return false;

        Stat stat = (Stat) o;

        if (hits != stat.hits) return false;
        if (err != stat.err) return false;
        if (max != stat.max) return false;
        if (min != stat.min) return false;
        if (compareDoubleLowPrecision(stat.avg, avg) != 0) return false;
        return children != null ? children.equals(stat.children) : stat.children == null;
    }

    public static int compareDoubleLowPrecision(double d1, double d2) {
        long l1 = Math.round(d1 * 1000);
        long l2 = Math.round(d2 * 1000);
        return Long.compare(l1, l2);
    }

    public static Stat emptyStat() {
        Stat s = new Stat();
        s.setMax(0);
        return s;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (hits ^ (hits >>> 32));
        result = 31 * result + (int) (err ^ (err >>> 32));
        result = 31 * result + (int) (max ^ (max >>> 32));
        result = 31 * result + (int) (min ^ (min >>> 32));
        temp = Double.doubleToLongBits(avg);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Stat{" +
                "hits=" + hits +
                ", err=" + err +
                ", max=" + max +
                ", min=" + min +
                ", avg=" + avg +
                ", children=" + children +
                '}';
    }
}
