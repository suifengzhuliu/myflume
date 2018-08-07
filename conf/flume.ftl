# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


# agentName the components on this agent
${agentName}.sources = source1
${agentName}.sinks = k1
${agentName}.channels = c1

# Describe/configure the source


<#if sourceType == 'HTTP'>
${agentName}.sources.source1.type = http
${agentName}.sources.source1.port = ${httpSource.port}
<#elseif sourceType == 'KAFKA' >
${agentName}.sources.source1.type = org.apache.flume.source.kafka.KafkaSource
${agentName}.sources.source1.bootstrap.servers = ${kafkaSource.servers}
${agentName}.sources.source1.topics = ${kafkaSource.topics}
${agentName}.sources.source1.consumer.group.id = ${kafkaSource.group}
		<#if kafkaSource.topicsRegex??>
${agentName}.sources.source1.regex = ${kafkaSource.topicsRegex}
	     </#if>
	     <#if kafkaSource.batchSize??>
${agentName}.sources.source1.batchSize = ${kafkaSource.batchSize}
	     </#if>
</#if> 





# Describe the sink
<#if sinkType == 'ES'>
${agentName}.sinks.k1.type = elasticsearch
	<#if esSink.indexagentName??>
${agentName}.sinks.k1.indexagentName = ${esSink.indexagentName}
	</#if>
	<#if esSink.indexType??>
${agentName}.sinks.k1.indexType = ${esSink.indexType}
	</#if>
		<#if esSink.clusteragentName??>
${agentName}.sinks.k1.clusteragentName = ${esSink.clusteragentName}
	</#if>
<#elseif sinkType == 'HDFS' >
${agentName}.sinks.k1.type = hdfs
${agentName}.sinks.k1.hdfs.path=${hdfsSink.path}
	<#if hdfsSink.filePrefix??>
${agentName}.sinks.k1.hdfs.filePrefix=${hdfsSink.filePrefix}
	</#if>
<#elseif sinkType == 'LOGGER' >
${agentName}.sinks.k1.type = logger
</#if>



${agentName}.channels.c1.type = file
${agentName}.channels.c1.checkpointDir = /tmp/flume/${agentName}/checkpoint
${agentName}.channels.c1.dataDirs = /tmp/flume/${agentName}/data


# Bind the source and sink to the channel
${agentName}.sources.source1.channels = c1
${agentName}.sinks.k1.channel = c1