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

#pragma once

#include <stdint.h>

#include <string>

#include "common/status.h"
#include "http/http_handler_with_auth.h"
#include "olap/tablet.h"

namespace doris {
class HttpRequest;

class ExecEnv;

enum class CompactionActionType {
    SHOW_INFO = 1,
    RUN_COMPACTION = 2,
    RUN_COMPACTION_STATUS = 3,
};

const std::string PARAM_COMPACTION_TYPE = "compact_type";
const std::string PARAM_COMPACTION_BASE = "base";
const std::string PARAM_COMPACTION_CUMULATIVE = "cumulative";
const std::string PARAM_COMPACTION_FULL = "full";
const std::string PARAM_COMPACTION_REMOTE = "remote";

/// This action is used for viewing the compaction status.
/// See compaction-action.md for details.
class CompactionAction : public HttpHandlerWithAuth {
public:
    CompactionAction(CompactionActionType ctype, ExecEnv* exec_env, StorageEngine& engine,
                     TPrivilegeHier::type hier, TPrivilegeType::type ptype);

    ~CompactionAction() override = default;

    void handle(HttpRequest* req) override;

private:
    Status _handle_show_compaction(HttpRequest* req, std::string* json_result);

    /// execute compaction request to run compaction task
    /// param compact_type in req to distinguish the task type, base or cumulative
    Status _handle_run_compaction(HttpRequest* req, std::string* json_result);

    /// thread callback function for the tablet to do compaction
    Status _execute_compaction_callback(TabletSharedPtr tablet, const std::string& compaction_type,
                                        bool fethch_from_remote);

    /// fetch compaction running status
    Status _handle_run_status_compaction(HttpRequest* req, std::string* json_result);

private:
    StorageEngine& _engine;
    CompactionActionType _compaction_type;
};

} // end namespace doris
