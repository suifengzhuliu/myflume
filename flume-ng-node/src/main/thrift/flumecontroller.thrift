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

struct ResponseState{
	1: optional Status status
	2: optional string msg
}

enum SourceType{
	HTTP,
	KAFKA,
	DB,
	HDFS
}

enum SinkType{
	ES,
	HDFS,
	KAFKA,
	LOGGER
}


struct HttpSource{
	1: required string port
}

struct KafkaSource{
	1: required string servers
	2: required string topics
	3: optional string group
}

struct HDFSSource{
	1: required string host
}


enum DBType{
MYSQL,
SQLSERVER,
ORACLE
}

struct DBSource{
	1: required string host
	2: required string port
	3: required string username
	4: required string password
	5: required string dbName
	6: required string sql
	7: required string interval
	8: required DBType type
	
}


struct ESSink{
	1: required string hostNames
	2: optional string indexName
	3: optional string indexType
	4: optional string clusterName
	5: optional string contentType
}

struct HDFSSink{
	1: required string path
	2: optional string filePrefix
}

struct KafkaSink{
	1: required string topic
	2: required string servers
}

struct FlumeSource {
	1:required SourceType sourceType
	2:optional HttpSource httpSource
	3:optional KafkaSource kafkaSource
	4:optional HDFSSource hdfsSource
	5:optional DBSource dbSource
	6: optional list<Interceptor> interceptorList
}


struct FlumeSink {
	1:required SinkType SinkType
	2:optional HDFSSink hdfsSink
	3:optional ESSink esSink
	4:optional KafkaSink kafkaSink
}


struct Interceptor{
	1:required string type
	2:optional map<string,string> params
}



struct FlumeAgent {                            
 
1:required string agentName
2: required list<FlumeSource> sourceList
3: required list<FlumeSink> sinkList
 
}

service   FlumeControllerService {
 
  ResponseState startFlumeAgent(1:FlumeAgent agent)
  ResponseState stopFlumeAgent(1:string agentName)
  //Override if exist
  ResponseState saveOrUpdateConf(1:FlumeAgent agent)
}

