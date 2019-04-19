package com.cgi.eoss.fstep.model;

import com.cgi.eoss.fstep.model.converters.FstepServiceDescriptorYamlConverter;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
/**
 * <p>The detailed service configuration required to complete a WPS service definition file.</p>
 * <p>All fields are broadly aligned with the official WPS spec as configured via ZOO-Project zcfg files.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FstepServiceDescriptor {

    private String id;

    private String title;

    private String description;

    private String version;

    private boolean storeSupported;

    private boolean statusSupported;

    private String serviceType;

    private String serviceProvider;

    private List<Parameter> dataInputs;

    private List<Parameter> dataOutputs;

    public String toYaml() {
        return FstepServiceDescriptorYamlConverter.toYaml(this);
    }

    public static FstepServiceDescriptor fromYaml(String yaml) {
        return FstepServiceDescriptorYamlConverter.fromYaml(yaml);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {

        public enum DataNodeType {
            LITERAL, COMPLEX, BOUNDING_BOX
        }

        private String id;

        private String title;

        private String description;

        private int minOccurs;

        private int maxOccurs;

        private DataNodeType data;

        @JsonInclude(Include.NON_NULL)
        private String timeRegexp;
        
        private Map<String, String> defaultAttrs;

        @Singular
        private List<Map<String, String>> supportedAttrs;
        
        @JsonInclude(Include.NON_NULL)
        private List<Relation> parameterRelations;
        
        @JsonInclude(Include.NON_NULL)
        private Map<String, String> platformMetadata;

    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Relation {
    	
    	public enum RelationType {
            VISUALIZATION_OF
        }
        private String targetParameterId;

        private RelationType type;
    }

}
