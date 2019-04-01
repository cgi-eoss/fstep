package com.cgi.eoss.fstep.rpc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Timestamp;

import lombok.experimental.UtilityClass;

/**
 * <p>Utility class providing helper methods for dealing with Protobuf/Grpc services and objects.</p>
 */
@UtilityClass
public class GrpcUtil {
    
    private static final int DEFAULT_FILE_STREAM_BUFFER_SIZE = 8192;
    
    /**
     * <p>Convert a gRPC parameters collection to a more convenient {@link Multimap}.</p>
     *
     * @param params The parameters to be converted.
     * @return The params input collection mapped to &lt;String, String&gt; entries.
     */
    public static Multimap<String, String> paramsListToMap(List<JobParam> params) {
        ImmutableMultimap.Builder<String, String> mapBuilder = ImmutableMultimap.builder();
        params.forEach(p -> mapBuilder.putAll(p.getParamName(), p.getParamValueList()));
        return mapBuilder.build();
    }

    /**
     * <p>Convert a {@link Multimap} into a collection of {@link JobParam}s for gRPC.</p>
     *
     * @param params The parameters to be converted.
     * @return The params input collection mapped to {@link JobParam}s.
     */
    public static List<JobParam> mapToParams(Multimap<String, String> params) {
        ImmutableList.Builder<JobParam> paramsBuilder = ImmutableList.builder();
        params.keySet().forEach(
                k -> paramsBuilder.add(JobParam.newBuilder().setParamName(k).addAllParamValue(params.get(k)).build()));
        return paramsBuilder.build();
    }

    /**
     * <p>Convert a {@link com.cgi.eoss.fstep.model.Job} to its gRPC {@link Job} representation.</p>
     *
     * @param job The job to be converted.
     * @return The input job mapped to {@link Job}.
     */
    public static Job toRpcJob(com.cgi.eoss.fstep.model.Job job) {
        return Job.newBuilder()
                .setId(job.getExtId())
                .setIntJobId(String.valueOf(job.getId()))
                .setUserId(job.getOwner().getName())
                .setServiceId(job.getConfig().getService().getName())
                .build();
    }
    
    /**
     * <p>Convert a {@link com.cgi.eoss.fstep.model.Service} to its gRPC {@link Service} representation.</p>
     *
     * @param job The service to be converted.
     * @return The input service mapped to {@link Service}.
     */
    public static Service toRpcService(com.cgi.eoss.fstep.model.FstepService service) {
        return Service.newBuilder()
        			.setId(String.valueOf(service.getId()))
                .setName(service.getName())
                .setDockerImageTag(service.getDockerTag())
                .build();
    }
    
    public static Timestamp timestampFromOffsetDateTime(OffsetDateTime offsetDateTime) {
    	Instant i = offsetDateTime.atZoneSameInstant(ZoneId.of("Z")).toInstant();
    	return Timestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();
    }
    
    public static OffsetDateTime offsetDateTimeFromTimestamp(Timestamp timestamp) {
    	Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    	return OffsetDateTime.ofInstant(instant, ZoneId.of("Z"));
    }
    
}
