package org.apache.maven.surefire.testng;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class TestNGProvider
    implements SurefireProvider
{
    private final Properties providerProperties;

    private final TestArtifactInfo testArtifactInfo;

    private final ReporterConfiguration reporterConfiguration;

    private final ReporterManagerFactory reporterManagerFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScannerParameters directoryScannerParameters;

    private final TestRequest testRequest;

    private final File basedir;

    public TestNGProvider( ProviderParameters booterParameters )
    {
        this.reporterManagerFactory = booterParameters.getReporterManagerFactory();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScannerParameters = booterParameters.getDirectoryScannerParameters();
        this.providerProperties = booterParameters.getProviderProperties();
        this.testRequest = booterParameters.getTestRequest();
        basedir = directoryScannerParameters != null ? directoryScannerParameters.getTestClassesDirectory() : null;
        testArtifactInfo = booterParameters.getTestArtifactInfo();
        reporterConfiguration = booterParameters.getReporterConfiguration();
    }

    public Boolean isRunnable()
    {
        return Boolean.TRUE;
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        SurefireTestSuite suite = getActiveSuite();
        suite.locateTestSets( testClassLoader );
        if ( forkTestSet != null && testRequest == null )
        {
            suite.execute( (String) forkTestSet, reporterManagerFactory, testClassLoader );
        }
        else
        {
            suite.execute( reporterManagerFactory, testClassLoader );
        }
        reporterManagerFactory.warnIfNoTests();
        return reporterManagerFactory.close();
    }

    boolean isTestNGXmlTestSuite( TestRequest testSuiteDefinition )
    {
        return testSuiteDefinition.getSuiteXmlFiles() != null && testSuiteDefinition.getSuiteXmlFiles().size() > 0 &&
            testSuiteDefinition.getRequestedTest() == null;

    }


    private TestNGDirectoryTestSuite getDirectorySuite()
    {
        return new TestNGDirectoryTestSuite( basedir, new ArrayList( directoryScannerParameters.getIncludes() ),
                                             new ArrayList( directoryScannerParameters.getExcludes() ),
                                             testRequest.getTestSourceDirectory().toString(),
                                             testArtifactInfo.getVersion(), testArtifactInfo.getClassifier(),
                                             providerProperties, reporterConfiguration.getReportsDirectory() );
    }

    private TestNGXmlTestSuite getXmlSuite()
    {
        return new TestNGXmlTestSuite( testRequest.getSuiteXmlFiles(), testRequest.getTestSourceDirectory().toString(),
                                       testArtifactInfo.getVersion(), testArtifactInfo.getClassifier(),
                                       providerProperties, reporterConfiguration.getReportsDirectory() );
    }


    public SurefireTestSuite getActiveSuite()
    {
        return isTestNGXmlTestSuite( testRequest ) ? (SurefireTestSuite) getXmlSuite() : getDirectorySuite();
    }

    public Iterator getSuites()
    {
        try
        {
            return getActiveSuite().locateTestSets( testClassLoader ).keySet().iterator();
        }
        catch ( TestSetFailedException e )
        {
            throw new RuntimeException( e );
        }
    }
}