package it.sotto.psd2.msa.massimali;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.Uri;
import com.typesafe.config.ConfigFactory;
import it.sotto.psd2.msa.CommonConstants;
import static it.sotto.psd2.msa.CommonConstants.ENV_HADOOP_USER_NAME;
import static it.sotto.psd2.msa.CommonConstants.ENV_HBASE_MASTER_PRINCIPAL;
import static it.sotto.psd2.msa.CommonConstants.ENV_HBASE_RS_PRINCIPAL;
import static it.sotto.psd2.msa.CommonConstants.ENV_KERBEROS_KEYTAB;
import static it.sotto.psd2.msa.CommonConstants.ENV_NO_KERBEROS;
import static it.sotto.psd2.msa.CommonConstants.ENV_SERVICE_URI;
import static it.sotto.psd2.msa.CommonConstants.ENV_URL_MS_ACCOUNT;
import static it.sotto.psd2.msa.CommonConstants.ENV_ZOOKEEPER_QUORUM;
import it.sotto.psd2.msa.Config;
import static it.sotto.psd2.msa.Config.getValue;
import it.sotto.psd2.msa.Microservice;
import it.sotto.psd2.msa.esb.ESBContrattoCorporate;
import it.sotto.psd2.msa.massimali.util.HBaseAccess;
import it.sotto.psd2.msa.massimali.util.WSMassimali;
import static it.sotto.psd2.msa.util.Utils.assertNotNull;
import java.math.BigDecimal;
import static java.util.Optional.ofNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Massimali implements Microservice {
	
	private static final Logger log = LoggerFactory.getLogger(Massimali.class);
	
	@Override
	public void validateEnvironment() {
		assertNotNull(ENV_SERVICE_URI);
		assertNotNull(ENV_ZOOKEEPER_QUORUM);
		
		assertNotNull(Constants.ENV_WEBSERVICE_CLASS);
		assertNotNull(Constants.ENV_WEBSERVICE_URL);
		assertNotNull(ENV_URL_MS_ACCOUNT);
		
		assertNotNull(Constants.ENV_DEFAULT_MAX_DAILY);
		assertPositiveNumber(Constants.ENV_DEFAULT_MAX_DAILY);

		assertNotNull(Constants.ENV_DEFAULT_MAX_MONTHLY);
		assertPositiveNumber(Constants.ENV_DEFAULT_MAX_MONTHLY);
		
		assertNotNull(Constants.ENV_MASSIMALI_TABLENAME);
		assertNotNull(Constants.ENV_MASSIMALI_CF_DETAILS);
		
		assertNotNull(CommonConstants.ENV_URL_MS_CRUSCOTTO);
		
		assertNotNull(CommonConstants.ENV_URL_WS_TERZEPARTI);

		if(!ofNullable(Config.getValue(ENV_NO_KERBEROS)).isPresent()) {
			assertNotNull(ENV_HBASE_MASTER_PRINCIPAL);
			assertNotNull(ENV_HBASE_RS_PRINCIPAL);
			assertNotNull(ENV_HADOOP_USER_NAME); 
			assertNotNull(ENV_KERBEROS_KEYTAB); 
		}
	}
	
	private void assertPositiveNumber(String envKey) {
		try {
			if((new BigDecimal(getValue(envKey))).compareTo(BigDecimal.ZERO)>0) {
				return;
			}
		} catch(Exception e) { }
		throw new IllegalStateException("env variable "+envKey+" does not contains a number greater than zero");	
	}
	
	protected WSMassimali createWSMassimali() {
		String wsClassName=Config.getValue(Constants.ENV_WEBSERVICE_CLASS);
		WSMassimali ws = null;
		try {
			ws=(WSMassimali)Class.forName(wsClassName).newInstance();
		} catch(ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			log.error("error creating ["+wsClassName+"] instance",e);
		}
		return ws;
	}

	@Override
	public void startService() {
		WSMassimali ws = createWSMassimali();
		if(ws!=null) {
			(new Service(
				ActorSystem.create(Constants.ACTOR_SYSTEM), 
				ConfigFactory.load(),
				Uri.create(Config.getValue(ENV_SERVICE_URI)),
				ws,
				new ESBContrattoCorporate(),
				new HBaseAccess()
			)).run();
		}
	}
}