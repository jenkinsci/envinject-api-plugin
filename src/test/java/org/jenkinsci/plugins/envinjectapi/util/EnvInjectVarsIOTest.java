package org.jenkinsci.plugins.envinjectapi.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvInjectVarsIOTest {

    @TempDir
    private File temporaryFolder;

    @Test
    void saveEnvironment() throws Exception {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("Key1", "Value1");
        envMap.put("Key 2", "Value 2");
        envMap.put("Key_3", "Value_3");

        EnvInjectVarsIO.saveEnvironment(temporaryFolder, envMap);

        assertEquals(envMap, EnvInjectVarsIO.getEnvironment(temporaryFolder));
    }
}
