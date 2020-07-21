package it.sotto.psd2.msa.massimali;

import it.sotto.psd2.msa.App;
import it.sotto.psd2.msa.CommonConstants;
import static it.sotto.psd2.msa.CommonConstants.ENV_HADOOP_USER_NAME;
import static it.sotto.psd2.msa.CommonConstants.ENV_HBASE_MASTER_PRINCIPAL;
import static it.sotto.psd2.msa.CommonConstants.ENV_HBASE_RS_PRINCIPAL;
import static it.sotto.psd2.msa.CommonConstants.ENV_KERBEROS_KEYTAB;
import static it.sotto.psd2.msa.CommonConstants.ENV_SERVICE_URI;
import static it.sotto.psd2.msa.CommonConstants.ENV_URL_MS_ACCOUNT;
import static it.sotto.psd2.msa.CommonConstants.ENV_ZOOKEEPER_QUORUM;
import it.sotto.psd2.msa.Config;
import it.sotto.psd2.msa.Microservice;
import it.sotto.psd2.msa.massimali.util.WSMassimali;

import java.lang.reflect.Method;

import org.apache.log4j.PropertyConfigurator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Config.class)
public class TestApp {
	private static final Class<?>[] classes = new Class<?>[]{String.class};
	
	private static final String MICROSERVICE_NAME = "massimali.Massimali";
	private static final String MICROSERVICE_WRONG_NAME = MICROSERVICE_NAME+"_";
	
	private Microservice ms = null;
	private Microservice wrongMs = null;
	
	public TestApp() {
		PropertyConfigurator.configure("log4j.properties");
		mockStatic(Config.class);
		when(Config.getValue(Config.ENV_LOG4J)).thenReturn(null);
		
		when(Config.getValue(ENV_HADOOP_USER_NAME)).thenReturn("hduser");
		when(Config.getValue(ENV_KERBEROS_KEYTAB)).thenReturn("keytab");
		when(Config.getValue(ENV_ZOOKEEPER_QUORUM)).thenReturn("localhost:1234");
		when(Config.getValue(ENV_HBASE_MASTER_PRINCIPAL)).thenReturn("master-principal");
		when(Config.getValue(ENV_HBASE_RS_PRINCIPAL)).thenReturn("rs-principal");

		when(Config.getValue(ENV_SERVICE_URI)).thenReturn("localhost:9080");
		when(Config.getValue(Constants.ENV_WEBSERVICE_CLASS)).thenReturn("webservice");
		when(Config.getValue(Constants.ENV_WEBSERVICE_URL)).thenReturn("localhost:1234");
		when(Config.getValue(ENV_URL_MS_ACCOUNT)).thenReturn("msaddress");
		when(Config.getValue(Constants.ENV_USERNAME_HEADER_WS)).thenReturn("wsuser");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("100");
		when(Config.getValue(Constants.ENV_MASSIMALI_TABLENAME)).thenReturn("massimatiTable");
		when(Config.getValue(Constants.ENV_MASSIMALI_CF_DETAILS)).thenReturn("details");
		when(Config.getValue(ENV_ZOOKEEPER_QUORUM)).thenReturn("zookeper.ced.it");
//		when(Config.getValue(CommonConstants.ENV_NO_KERBEROS)).thenReturn("");
		when(Config.getValue(CommonConstants.ENV_URL_MS_CRUSCOTTO)).thenReturn("localhost:1234");
		when(Config.getValue(CommonConstants.ENV_URL_WS_TERZEPARTI)).thenReturn("localhost:1234");
		
		try {
			Method method = App.class.getDeclaredMethod("createMicroservice", classes);
			method.setAccessible(true);
			ms=(Microservice)method.invoke(null, new Object[]{MICROSERVICE_NAME});
			wrongMs=(Microservice)method.invoke(null, new Object[]{MICROSERVICE_WRONG_NAME});
		} catch (Exception e) {
			
		}
	}
	
	@Test 
	public void testMicroserviceCreation() {
		assertNotNull("microservice not created", ms);
		assertNull("microservice created", wrongMs);
	}

	@Test 
	public void testEnvironmentValidation() {
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("100");

		ms.validateEnvironment();
	}
	
	@Test(expected = IllegalStateException.class)
	public void testEnvironmentValidation_maxDailyError() {
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("a");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("100");
		
		ms.validateEnvironment();
	}
	
	@Test(expected = IllegalStateException.class)
	public void testEnvironmentValidation_maxMonthlyError() {
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_DAILY)).thenReturn("10");
		when(Config.getValue(Constants.ENV_DEFAULT_MAX_MONTHLY)).thenReturn("a");
		
		ms.validateEnvironment();
	}

	@Test 
	public void testCreateMassimaliImplementation() {
		Massimali ms = new Massimali();

		WSMassimali wsImpl = null;
		String className;
		
		className="it.sotto.psd2.msa.massimali.util.WSMassimaliService";
		when(Config.getValue(Constants.ENV_WEBSERVICE_CLASS)).thenReturn(className);
		
		wsImpl = ms.createWSMassimali();
		assertNotNull(wsImpl);
		assertEquals(className,wsImpl.getClass().getName());
		
		className="it.sotto.psd2.msa.massimali.util.FakeWS__";
		when(Config.getValue(Constants.ENV_WEBSERVICE_CLASS)).thenReturn(className);

		wsImpl = ms.createWSMassimali();
		assertNull(wsImpl);
	}
	
	@Test(expected = Exception.class)
	public void testCreateMassimaliImplementation_exception() {
		when(Config.getValue(Constants.ENV_WEBSERVICE_CLASS)).thenReturn(null);
		
		(new Massimali()).createWSMassimali();
	}

	@Test
	public void testStartService() {
		when(Config.getValue(Constants.ENV_WEBSERVICE_CLASS)).thenReturn("WEBSERVICE_CLASS");

		(new Massimali()).startService();
	}

	@Test(expected = Exception.class)
	public void testStartService_exception() {
		when(Config.getValue(Constants.ENV_WEBSERVICE_CLASS)).thenReturn(null);

		(new Massimali()).startService();
	}

	@Test(expected = Exception.class)
	public void testStartService_InvocationTargetException() {
		Massimali ms = new Massimali();

		WSMassimali wsImpl = null;
		String className;

		className="it.sotto.psd2.msa.massimali.util.WSMassimaliService";
		when(Config.getValue(Constants.ENV_WEBSERVICE_CLASS)).thenReturn(className);

		wsImpl = ms.createWSMassimali();

		ms.startService();
	}
}
