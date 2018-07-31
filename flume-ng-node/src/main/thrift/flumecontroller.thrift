/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

namespace java org.apache.flume.service

enum Status {
  OK,
  FAILED
}


enum SourceType{
	HTTP,
	KAFKA,
	DB,
	HDFS
}

enum SinkType{
	ES,
	HDFS
}


struct HttpSource{
	1: required i32 port
}

struct KafkaSource{
	1: required string servers
	2: required string topics
	3: required string group
	4: optional string topicsRegex
	5: optional i32 batchSize
}

struct HDFSSource{
	1: required string host
}

struct DBSource{
	1: required string host
	2: required i32 port
	3: required string username
	4: required string password
	5: required string dbName
	6: required string sql
	7: required i32 interval
}


struct ESSink{
	1: required string hostNames
	2: optional string indexName
	3: optional string indexType
	4: optional string clusterName
}

struct HDFSSink{
	1: required string path
	2: optional string filePrefix
}


struct FlumeAgent {                            
 
1:required string name
2: required SourceType sourceType
3: required SinkType SinkType
 
4:optional HttpSource httpSource
5:optional KafkaSource kafkaSource
6:optional HDFSSource hdfsSource
7:optional DBSource dbSource
 
 
8:optional HDFSSink hdfsSink
9:optional ESSink esSink
 
}

service   FlumeControllerService {
 
  Status startFlumeAgent(1:string name 2:FlumeAgent agent)
  Status stopFlumeAgent(1:string name)
  //Override if exist
  Status modifyConf(1:string name 2:FlumeAgent agent)
}

