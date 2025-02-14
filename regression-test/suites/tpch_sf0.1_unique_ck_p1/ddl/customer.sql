CREATE TABLE IF NOT EXISTS customer (
  C_CUSTKEY     INTEGER NOT NULL,
  C_NAME        VARCHAR(25) NOT NULL,
  C_ADDRESS     VARCHAR(40) NOT NULL,
  C_NATIONKEY   INTEGER NOT NULL,
  C_PHONE       CHAR(15) NOT NULL,
  C_ACCTBAL     DECIMAL(15,2)   NOT NULL,
  C_MKTSEGMENT  CHAR(10) NOT NULL,
  C_COMMENT     VARCHAR(117) NOT NULL
)
UNIQUE KEY(C_CUSTKEY)
CLUSTER BY(C_PHONE, C_ADDRESS)
DISTRIBUTED BY HASH(C_CUSTKEY) BUCKETS 1
PROPERTIES (
  "replication_num" = "1"
)

