import com.cgi.eoss.fstep.wps.FstepServicesClient;

import java.util.HashMap;

public class ${id} {

    public static int ${id}(HashMap conf, HashMap inputs, HashMap outputs) {
        return FstepServicesClient.launch("${id}", conf, inputs, outputs);
    }

}