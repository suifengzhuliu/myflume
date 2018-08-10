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

<#assign sourceIndexs = ""/>
<#assign sinkIndexs = ""/>
<#list sourceList as flumesource>
	<#assign sourceIndexs = sourceIndexs +" "+ "source${flumesource_index}"/>
</#list>

<#list sinkList as flumesink>
	<#assign sinkIndexs = sinkIndexs +" "+ "sink${flumesink_index}"/>
</#list>


${agentName}.sources = ${sourceIndexs}
${agentName}.sinks = ${sinkIndexs}
${agentName}.channels = c1

# Describe/configure the source


<#list sourceList as flumesource>

	<#if flumesource.sourceType == 'HTTP'>
			${agentName}.sources.source${flumesource_index}.type = http
			${agentName}.sources.source${flumesource_index}.port = ${flumesource.httpSource.port}
	<#elseif flumesource.sourceType == 'KAFKA' >
			${agentName}.sources.source${flumesource_index}.type = org.apache.flume.source.kafka.KafkaSource
			${agentName}.sources.source${flumesource_index}.bootstrap.servers = ${flumesource.kafkaSource.servers}
			${agentName}.sources.source${flumesource_index}.topics = ${flumesource.kafkaSource.topics}
			${agentName}.sources.source${flumesource_index}.consumer.group.id = ${flumesource.kafkaSource.group}
			<#if flumesource.kafkaSource.topicsRegex??>
					    ${agentName}.sources.source${flumesource_index}.regex = ${flumesource.kafkaSource.topicsRegex}
		     </#if>
		     <#if flumesource.kafkaSource.batchSize??>
						${agentName}.sources.source${flumesource_index}.batchSize = ${flumesource.kafkaSource.batchSize}
		     </#if>
	</#if> 

      <#if flumesource.interceptorList?? && (flumesource.interceptorList?size > 0)  >
      		<#assign interIndexs = ""/>
	
      		<#list flumesource.interceptorList as interceptor>
      		<#assign interIndexs = interIndexs +" "+ "i${interceptor_index}"/>
      		
      			 					${agentName}.sources.source${flumesource_index}.interceptors.i${interceptor_index}.type = ${interceptor.type}
      			  <#if interceptor.params??>
      			  			<#list interceptor.params?keys as key>
      			  					${agentName}.sources.source${flumesource_index}.interceptors.i${interceptor_index}.${key!} = ${interceptor.params[key]!}
      			  			</#list>
      			  </#if>
            </#list>
            
            						 ${agentName}.sources.source${flumesource_index}.interceptors = ${interIndexs}
	  </#if> 
</#list>


# Describe the sink

<#list sinkList as flumesink>

	<#if flumesink.sinkType == 'ES'>
				${agentName}.sinks.sink${flumesink_index}.type = elasticsearch
		   <#if flumesink.esSink.hostNames??>
				${agentName}.sinks.sink${flumesink_index}.hostNames = ${flumesink.esSink.hostNames}
		  </#if>
		  <#if flumesink.esSink.indexName??>
				${agentName}.sinks.sink${flumesink_index}.indexName = ${flumesink.esSink.indexName}
		  </#if>
		   <#if flumesink.esSink.indexType??>
				${agentName}.sinks.sink${flumesink_index}.indexType = ${flumesink.esSink.indexType}
		   </#if>
			<#if flumesink.esSink.clusterName??>
				${agentName}.sinks.sink${flumesink_index}.clusterName = ${flumesink.esSink.clusterName}
		    </#if>
	<#elseif flumesink.sinkType == 'HDFS' >
				${agentName}.sinks.sink${flumesink_index}.type = hdfs
				${agentName}.sinks.sink${flumesink_index}.hdfs.path=${flumesink.hdfsSink.path}
			<#if flumesink.hdfsSink.filePrefix??>
				${agentName}.sinks.sink${flumesink_index}.hdfs.filePrefix=${flumesink.hdfsSink.filePrefix}
			</#if>
	<#elseif flumesink.sinkType == 'LOGGER' >
				${agentName}.sinks.sink${flumesink_index}.type = logger
	<#elseif flumesink.sinkType == 'KAFKA' >
				${agentName}.sinks.sink${flumesink_index}.type = org.apache.flume.sink.kafka.KafkaSink
				${agentName}.sinks.sink${flumesink_index}.kafka.topic = ${flumesink.kafkaSink.topic}
				${agentName}.sinks.sink${flumesink_index}.kafka.bootstrap.servers  = ${flumesink.kafkaSink.servers}
	</#if>

</#list>


${agentName}.channels.c1.type = file
${agentName}.channels.c1.checkpointDir = /tmp/flume/${agentName}/checkpoint
${agentName}.channels.c1.dataDirs = /tmp/flume/${agentName}/data

# Bind the source and sink to the channel
<#list sourceList as flumesource>
	${agentName}.sources.source${flumesource_index}.channels = c1
</#list>
<#list sinkList as flumesink>
	${agentName}.sinks.sink${flumesink_index}.channel = c1
</#list>