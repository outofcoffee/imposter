package io.gatehill.imposter.plugin.hbase.model;

import java.util.Comparator;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResultCellComparator implements Comparator<ResultCell> {
    @Override
    public int compare(ResultCell o1, ResultCell o2) {
        final int nameCompare = o1.getFieldName().compareTo(o2.getFieldName());
        if (nameCompare == 0) {
            return o1.getFieldValue().compareTo(o2.getFieldValue());
        } else {
            return nameCompare;
        }
    }
}
