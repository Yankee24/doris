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

package org.apache.doris.persist;

import org.apache.doris.common.io.Text;
import org.apache.doris.common.io.Writable;
import org.apache.doris.persist.gson.GsonUtils;

import com.google.gson.annotations.SerializedName;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AutoIncrementIdUpdateLog implements Writable {
    @SerializedName(value = "dbId")
    private Long dbId;
    @SerializedName(value = "tableId")
    private Long tableId;
    @SerializedName(value = "columnId")
    private Long columnId;
    @SerializedName(value = "batchEndId")
    private Long batchEndId;

    public AutoIncrementIdUpdateLog(Long dbId, Long tableId, Long columnId, Long batchEndId) {
        this.dbId = dbId;
        this.tableId = tableId;
        this.columnId = columnId;
        this.batchEndId = batchEndId;
    }

    public Long getDbId() {
        return dbId;
    }

    public Long getTableId() {
        return tableId;
    }

    public Long getColumnId() {
        return columnId;
    }

    public long getBatchEndId() {
        return batchEndId;
    }

    public static AutoIncrementIdUpdateLog read(DataInput in) throws IOException {
        return GsonUtils.GSON.fromJson(Text.readString(in), AutoIncrementIdUpdateLog.class);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Text.writeString(out, GsonUtils.GSON.toJson(this));
    }
}
