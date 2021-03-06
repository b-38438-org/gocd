/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.plugin.infra.plugininfo;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;

@Component
public class GoPluginDescriptorBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoPluginDescriptorBuilder.class);

    private static final String PLUGIN_XML = "plugin.xml";
    private SystemEnvironment systemEnvironment;
    private File bundlePathLocation;

    protected GoPluginDescriptorBuilder() {
        this.systemEnvironment = new SystemEnvironment();
        bundlePathLocation = bundlePath();
    }

    @Autowired
    public GoPluginDescriptorBuilder(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        bundlePathLocation = bundlePath();
    }

    public GoPluginDescriptor build(File pluginJarFile, boolean isBundledPlugin) {
        if (!pluginJarFile.exists()) {
            throw new RuntimeException(String.format("Plugin jar does not exist: %s", pluginJarFile.getAbsoluteFile()));
        }
        try (JarFile jarFile = new JarFile(pluginJarFile)) {
            ZipEntry entry = jarFile.getEntry(PLUGIN_XML);

            if (entry == null) {
                return GoPluginDescriptor.usingId(pluginJarFile.getName(), pluginJarFile.getAbsolutePath(), getBundleLocation(bundlePathLocation, pluginJarFile.getName()), isBundledPlugin);
            }

            try (InputStream pluginXMLStream = jarFile.getInputStream(entry)) {
                return GoPluginDescriptorParser.parseXML(pluginXMLStream, pluginJarFile.getAbsolutePath(), getBundleLocation(bundlePathLocation, pluginJarFile.getName()), isBundledPlugin);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not load plugin with jar filename:{}", pluginJarFile.getName(), e);
            String cause = e.getCause() != null ? String.format("%s. Cause: %s", e.getMessage(), e.getCause().getMessage()) : e.getMessage();
            return GoPluginDescriptor.usingId(pluginJarFile.getName(), pluginJarFile.getAbsolutePath(), getBundleLocation(bundlePathLocation, pluginJarFile.getName()), isBundledPlugin)
                    .markAsInvalid(Arrays.asList(String.format("Plugin with ID (%s) is not valid: %s", pluginJarFile.getName(), cause)), e);
        }
    }

    private File getBundleLocation(File bundleDirectory, String name) {
        return new File(bundleDirectory, name);
    }

    File bundlePath() {
        File bundleDir = new File(systemEnvironment.get(PLUGIN_BUNDLE_PATH));
        FileUtil.validateAndCreateDirectory(bundleDir);
        return bundleDir;
    }
}
