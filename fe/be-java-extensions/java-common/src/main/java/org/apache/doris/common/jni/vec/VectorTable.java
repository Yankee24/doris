// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.common.jni.vec;


import org.apache.doris.common.jni.utils.OffHeap;
import org.apache.doris.common.jni.vec.ColumnType.Type;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Map;

/**
 * Store a batch of data as vector table.
 */
public class VectorTable {
    public static final Logger LOG = Logger.getLogger(VectorTable.class);
    private final VectorColumn[] columns;
    private final ColumnType[] columnTypes;
    private final String[] fields;
    private final VectorColumn meta;
    private final boolean onlyReadable;
    private final int numRowsOfReadable;

    // Create writable vector table
    private VectorTable(ColumnType[] types, String[] fields, int capacity) {
        this.columnTypes = types;
        this.fields = fields;
        this.columns = new VectorColumn[types.length];
        int metaSize = 1; // number of rows
        for (int i = 0; i < types.length; i++) {
            columns[i] = VectorColumn.createWritableColumn(types[i], capacity);
            metaSize += types[i].metaSize();
        }
        this.meta = VectorColumn.createWritableColumn(new ColumnType("#meta", Type.BIGINT), metaSize);
        this.onlyReadable = false;
        numRowsOfReadable = -1;
    }

    // Create readable vector table
    // `metaAddress` is generated by `JniConnector::generate_meta_info`
    private VectorTable(ColumnType[] types, String[] fields, long metaAddress) {
        long address = metaAddress;
        this.columnTypes = types;
        this.fields = fields;
        this.columns = new VectorColumn[types.length];

        int numRows = (int) OffHeap.getLong(null, address);
        address += 8;
        int metaSize = 1; // stores the number of rows + other columns meta data
        for (int i = 0; i < types.length; i++) {
            columns[i] = VectorColumn.createReadableColumn(types[i], numRows, address);
            metaSize += types[i].metaSize();
            address += types[i].metaSize() * 8L;
        }
        this.meta = VectorColumn.createReadableColumn(metaAddress, metaSize, new ColumnType("#meta", Type.BIGINT));
        this.onlyReadable = true;
        numRowsOfReadable = numRows;
    }

    public static VectorTable createWritableTable(ColumnType[] types, String[] fields, int capacity) {
        return new VectorTable(types, fields, capacity);
    }

    public static VectorTable createWritableTable(Map<String, String> params, int capacity) {
        String[] requiredFields = params.get("required_fields").split(",");
        String[] types = params.get("columns_types").split("#");
        ColumnType[] columnTypes = new ColumnType[types.length];
        for (int i = 0; i < types.length; i++) {
            columnTypes[i] = ColumnType.parseType(requiredFields[i], types[i]);
        }
        return createWritableTable(columnTypes, requiredFields, capacity);
    }

    public static VectorTable createWritableTable(Map<String, String> params) {
        return createWritableTable(params, Integer.parseInt(params.get("num_rows")));
    }

    public static VectorTable createReadableTable(ColumnType[] types, String[] fields, long metaAddress) {
        return new VectorTable(types, fields, metaAddress);
    }

    public static VectorTable createReadableTable(Map<String, String> params) {
        if (params.get("required_fields").isEmpty()) {
            assert params.get("columns_types").isEmpty();
            return createReadableTable(new ColumnType[0], new String[0], Long.parseLong(params.get("meta_address")));
        }
        String[] requiredFields = params.get("required_fields").split(",");
        String[] types = params.get("columns_types").split("#");
        long metaAddress = Long.parseLong(params.get("meta_address"));
        // Get sql string from configuration map
        ColumnType[] columnTypes = new ColumnType[types.length];
        for (int i = 0; i < types.length; i++) {
            columnTypes[i] = ColumnType.parseType(requiredFields[i], types[i]);
        }
        return createReadableTable(columnTypes, requiredFields, metaAddress);
    }

    public void appendNativeData(int fieldId, NativeColumnValue o) {
        assert (!onlyReadable);
        columns[fieldId].appendNativeValue(o);
    }

    public void appendData(int fieldId, ColumnValue o) {
        assert (!onlyReadable);
        columns[fieldId].appendValue(o);
    }

    public void appendData(int fieldId, Object[] batch, ColumnValueConverter converter, boolean isNullable) {
        assert (!onlyReadable);
        if (converter != null) {
            columns[fieldId].appendObjectColumn(converter.convert(batch), isNullable);
        } else {
            columns[fieldId].appendObjectColumn(batch, isNullable);
        }
    }

    public void appendData(int fieldId, Object[] batch, boolean isNullable) {
        appendData(fieldId, batch, null, isNullable);
    }

    /**
     * Get materialized data, each type is wrapped by its Java type. For example: int -> Integer, decimal -> BigDecimal
     *
     * @param converters A map of converters. Convert the column values if the type is not defined in ColumnType.
     * The map key is the field ID in VectorTable.
     */
    public Object[][] getMaterializedData(int start, int end, Map<Integer, ColumnValueConverter> converters) {
        if (columns.length == 0) {
            return new Object[0][0];
        }
        Object[][] data = new Object[columns.length][];
        for (int j = 0; j < columns.length; ++j) {
            Object[] columnData = columns[j].getObjectColumn(start, end);
            if (converters.containsKey(j)) {
                data[j] = converters.get(j).convert(columnData);
            } else {
                data[j] = columnData;
            }
        }
        return data;
    }

    public Object[][] getMaterializedData(Map<Integer, ColumnValueConverter> converters) {
        return getMaterializedData(0, getNumRows(), converters);
    }

    public Object[][] getMaterializedData() {
        return getMaterializedData(Collections.emptyMap());
    }

    public VectorColumn[] getColumns() {
        return columns;
    }

    public VectorColumn getColumn(int fieldId) {
        return columns[fieldId];
    }

    public ColumnType getColumnType(int fieldId) {
        return columnTypes[fieldId];
    }

    public ColumnType[] getColumnTypes() {
        return columnTypes;
    }

    public String[] getFields() {
        return fields;
    }

    public void releaseColumn(int fieldId) {
        assert (!onlyReadable);
        columns[fieldId].close();
    }

    public int getNumRows() {
        if (onlyReadable) {
            return numRowsOfReadable;
        } else {
            return columns[0].numRows();
        }
    }

    public int getNumColumns() {
        return columns.length;
    }

    public boolean isConstColumn(int idx) {
        return columns[idx].isConst();
    }

    public long getMetaAddress() {
        if (!onlyReadable) {
            meta.reset();
            meta.appendLong(getNumRows());
            for (VectorColumn c : columns) {
                c.updateMeta(meta);
            }
        }
        return meta.dataAddress();
    }

    public void reset() {
        assert (!onlyReadable);
        for (VectorColumn column : columns) {
            column.reset();
        }
        meta.reset();
    }

    public void close() {
        assert (!onlyReadable);
        for (int i = 0; i < columns.length; i++) {
            releaseColumn(i);
        }
        meta.close();
    }

    // for test only.
    public String dump(int rowLimit) {
        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < columns.length; col++) {
            ColumnType.Type typeValue = columns[col].getColumnPrimitiveType();
            sb.append(typeValue.name());
            sb.append("(rows: " + columns[col].numRows());
            sb.append(")(const: ");
            sb.append(columns[col].isConst() ? "true) " : "false) ");
            if (col != 0) {
                sb.append(",    ");
            }
        }
        sb.append("\n");

        for (int i = 0; i < rowLimit && i < getNumRows(); i++) {
            for (int j = 0; j < columns.length; j++) {
                if (j != 0) {
                    sb.append(", ");
                }
                columns[j].dump(sb, i);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
