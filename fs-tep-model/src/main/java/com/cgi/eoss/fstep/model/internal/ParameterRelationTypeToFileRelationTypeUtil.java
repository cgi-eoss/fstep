package com.cgi.eoss.fstep.model.internal;

import com.cgi.eoss.fstep.model.FstepFilesRelation;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Relation.RelationType;

public class ParameterRelationTypeToFileRelationTypeUtil {

	public static FstepFilesRelation.Type fromParameterRelationType(RelationType relationType) {
		switch (relationType) {
		case VISUALIZATION_OF: 
			return FstepFilesRelation.Type.VISUALIZATION_OF;
		default:
			return null;
		}
	}
}
