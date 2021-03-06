/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.service.AgentUpgradeService;
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.agent.statusapi.AgentHealthHolder;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.apache.http.client.HttpClient;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

import static com.thoughtworks.go.util.SystemUtil.getFirstLocalNonLoopbackIpAddress;
import static com.thoughtworks.go.util.SystemUtil.getLocalhostName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class AgentControllerTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    @Mock
    private BuildRepositoryRemote loopServer;
    @Mock
    private GoArtifactsManipulator artifactsManipulator;
    @Mock
    private SslInfrastructureService sslInfrastructureService;
    @Mock
    private Work work;
    @Mock
    private SubprocessLogger subprocessLogger;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private AgentUpgradeService agentUpgradeService;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Mock
    private HttpService httpService;
    @Mock
    private HttpClient httpClient;
    private AgentController agentController;

    private String agentUuid = "uuid";

    @Mock
    private AgentRegistry agentRegistry;
    private TestingClock clock = new TestingClock();
    private final int pingInterval = 5000;
    private AgentHealthHolder agentHealthHolder = new AgentHealthHolder(clock, pingInterval);

    private AgentIdentifier agentIdentifier;

    @BeforeEach
    void setUp() {
        initMocks(this);
        agentIdentifier = new AgentIdentifier(getLocalhostName(), getFirstLocalNonLoopbackIpAddress(), agentUuid);
    }

    @Test
    void shouldReturnTrueIfCausedBySecurity() {
        Exception exception = new Exception(new RuntimeException(new GeneralSecurityException()));

        agentController = createAgentController();
        assertThat(agentController.isCausedBySecurity(exception)).isTrue();
    }

    @Test
    void shouldReturnFalseIfNotCausedBySecurity() {
        Exception exception = new Exception(new IOException());
        agentController = createAgentController();
        assertThat(agentController.isCausedBySecurity(exception)).isFalse();
    }

    @Test
    void shouldUpgradeAgentBeforeAgentRegistration() throws Exception {
        agentController = createAgentController();
        InOrder inOrder = inOrder(agentUpgradeService, sslInfrastructureService);
        agentController.loop();
        inOrder.verify(agentUpgradeService).checkForUpgradeAndExtraProperties();
        inOrder.verify(sslInfrastructureService).registerIfNecessary(agentController.getAgentAutoRegistrationProperties());
    }

    @Test
    void remembersLastPingTime() throws Exception {
        // initial time
        Date now = new Date(42);
        clock.setTime(now);
        agentController = createAgentController();
        agentController.pingSuccess();

        assertThat(agentHealthHolder.hasLostContact()).isFalse();
        clock.addMillis(pingInterval);
        assertThat(agentHealthHolder.hasLostContact()).isFalse();
        clock.addMillis(pingInterval);
        assertThat(agentHealthHolder.hasLostContact()).isTrue();
    }

    private AgentController createAgentController() {
        return new AgentController(sslInfrastructureService, systemEnvironment, agentRegistry, pluginManager,
                subprocessLogger, agentUpgradeService, agentHealthHolder) {
            @Override
            public void ping() {

            }

            @Override
            public void execute() {

            }

            @Override
            protected void work() throws Exception {

            }
        };
    }
}
