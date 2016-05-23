/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/


package org.wso2.carbon.bpmn.tests.osgi;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.bpmn.core.BPMNEngineService;
import org.wso2.carbon.bpmn.core.deployment.BPMNDeployer;
import org.wso2.carbon.bpmn.tests.osgi.utils.BasicServerConfigurationUtil;
import org.wso2.carbon.deployment.engine.Artifact;
import org.wso2.carbon.deployment.engine.ArtifactType;
import org.wso2.carbon.deployment.engine.exception.CarbonDeploymentException;
import org.wso2.carbon.kernel.utils.CarbonServerInfo;
import org.wso2.carbon.osgi.test.util.CarbonSysPropConfiguration;
import org.wso2.carbon.osgi.test.util.OSGiTestConfigurationUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.inject.Inject;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BPMNServerCreationTest {

    private static final Log log = LogFactory.getLog(BPMNServerCreationTest.class);

    @Inject
    private BundleContext bundleContext;

    @Inject
    private BPMNEngineService bpmnEngineService;

    @Inject
    private BPMNDeployer bpmnDeployer;

    @Inject
    private CarbonServerInfo carbonServerInfo;


    @Configuration
    public Option[] createConfiguration() {

        List<Option> optionList = BasicServerConfigurationUtil.createBasicConfiguration();
        Path carbonHome = BasicServerConfigurationUtil.getCarbonHome();

        CarbonSysPropConfiguration sysPropConfiguration = new CarbonSysPropConfiguration();
        sysPropConfiguration.setCarbonHome(carbonHome.toString());
        sysPropConfiguration.setServerKey("carbon-bpmn");
        sysPropConfiguration.setServerName("WSO2 Carbon BPMN Server");
        sysPropConfiguration.setServerVersion("1.0.0");

        optionList = OSGiTestConfigurationUtils.getConfiguration(optionList, sysPropConfiguration);
        Option[] options = optionList.toArray(new Option[optionList.size()]);
        return options;
    }

    @Test(priority = 0)
    public void testProcessEngineCreation() {
        log.trace("[Test] Process engine creation : Started");
        ProcessEngine processEngine = bpmnEngineService.getProcessEngine();
        Assert.assertNotNull(processEngine, "processEngine is not set");
        String name = processEngine.getName();
        Assert.assertNotNull(name, "processEngine name is null.");
        log.trace("[Test] Process engine creation : Completed..");
    }

    @Test(priority = 10)
    public void testHelloWorldBarDeployment() throws CarbonDeploymentException {
        log.trace("[Test] Deployment test - HelloWorld.bar : Started");
        File ab = new File(Paths.get(BasicServerConfigurationUtil.getArtifactHome().toString(), "HelloWorld.bar")
                .toString());
        Artifact artifact = new Artifact(ab);
        ArtifactType artifactType = new ArtifactType<>("bar");
        artifact.setKey("HelloWorld.bar");
        artifact.setType(artifactType);
        bpmnDeployer.deploy(artifact);

        RepositoryService repositoryService = bpmnEngineService.getProcessEngine().getRepositoryService();
        List<Deployment> activitiDeployments = repositoryService.createDeploymentQuery().list();
        if (activitiDeployments != null) {
            Assert.assertEquals(activitiDeployments.size(), 1, "Expected Deployment count");
            Deployment deployment = activitiDeployments.get(0);
            Assert.assertTrue(artifact.getName().toString().startsWith(deployment.getName()), "Artifact Name " +
                    "mismatched.");
        } else {
            Assert.fail("There is no artifacts deployed.");
        }
        log.trace("[Test] Deployment test - HelloWorld.bar : Completed");
    }

    @Test(priority = 11)
    public void testStartHelloWorldBarProcess() {
        ProcessEngine processEngine = bpmnEngineService.getProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.startProcessInstanceByKey("helloWorldProcess");
    }
}
