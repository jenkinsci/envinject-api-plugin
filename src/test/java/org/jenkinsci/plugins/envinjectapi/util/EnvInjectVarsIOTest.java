package org.jenkinsci.plugins.envinjectapi.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EnvInjectVarsIOTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void saveEnvironment() throws Exception {
        File tmpDir = temporaryFolder.newFolder();
        Map<String, String> envMap = new HashMap<>();
        envMap.put("Key1", "Value1");
        envMap.put("Key 2", "Value 2");
        envMap.put("Key_3", "Value_3");

        EnvInjectVarsIO.saveEnvironment(tmpDir, envMap);

        assertEquals(envMap, EnvInjectVarsIO.getEnvironment(tmpDir));
    }
}
