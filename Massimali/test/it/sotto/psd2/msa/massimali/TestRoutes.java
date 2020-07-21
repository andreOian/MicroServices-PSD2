package it.sotto.psd2.msa.massimali;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import com.typesafe.config.ConfigFactory;
import static it.sotto.psd2.msa.CommonConstants.HEADER_CLIENT_ID;
import static it.sotto.psd2.msa.CommonConstants.HEADER_LANG;
import static it.sotto.psd2.msa.CommonConstants.HEADER_SCOPE;
import static it.sotto.psd2.msa.CommonConstants.HEADER_TPP_NAME;
import static it.sotto.psd2.msa.CommonConstants.HEADER_USER_CODE;
import static it.sotto.psd2.msa.CommonConstants.HEADER_USER_ID;
import static it.sotto.psd2.msa.CommonConstants.HEADER_USER_TYPE;
import static it.sotto.psd2.msa.CommonConstants.HEADER_UUID;
import it.sotto.psd2.msa.esb.ESBContrattoCorporate;
import it.sotto.psd2.msa.massimali.util.MassimaliDataAccess;
import it.sotto.psd2.msa.massimali.util.WSMassimali;
import it.sotto.psd2.msa.model.AccountsModel;
import it.sotto.psd2.msa.model.IbanModel;
import it.sotto.psd2.msa.model.cruscotto.KafkaMessageModel;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.log4j.PropertyConfigurator;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Oian Andrea
 * 
 */
public class TestRoutes extends JUnitRouteTest {

	protected final MassimaliDataAccess db = mock(MassimaliDataAccess.class);
	protected final WSMassimali ws = mock(WSMassimali.class);
	protected final ESBContrattoCorporate esbCorporate = mock(ESBContrattoCorporate.class);
	
	protected Service service = null;
	protected TestRoute testRoute = null;
	
	protected static final String uuid = "123";
	protected static final String lang = "it";
	protected static final String abi = "12345";
	protected static final String user_id = "id";
	protected static final String user_code = "8888";
	protected static final String iban1 = "IT0000000000000001";
	protected static final String iban2 = "IT0000000000000002";
	protected static final int istituto3n = 999;
	protected static final int servizio = 3;
	
	protected AccountsModel accounts;

	public TestRoutes() {
		PropertyConfigurator.configure("log4j.properties");
		
		accounts = createAccountsModel(iban1,iban2);
		
		service = new Service(
			ActorSystem.create(Constants.ACTOR_SYSTEM), 
			ConfigFactory.load(),
			Uri.create("http://localhost:9080"), 
			ws, esbCorporate, db
		){
			@Override
			public AccountsModel getAccounts(
				boolean berlinGroup, 
				String uuid, String abi, String userId, String userType, String userCode, String accountId, 
				Optional<String> scope, 
				HttpRequest request
			) {
				return accounts;
			}
			@Override
			protected void logNRT(String uuid, KafkaMessageModel message) {
				;
			}
		};
		
		testRoute = testRoute(service.getRoutes());
	}
	
	protected HttpRequest createGet(
		String url, String user_type, 
		Optional<String> scope, Optional<String> clientId, Optional<String> tppName
	) {
		HttpRequest request = HttpRequest.GET(url)
			.addHeader(RawHeader.create(HEADER_LANG, lang))
			.addHeader(RawHeader.create(HEADER_UUID, uuid))
			.addHeader(RawHeader.create(HEADER_USER_ID, user_id))
			.addHeader(RawHeader.create(HEADER_USER_CODE, user_code))
			.addHeader(RawHeader.create(HEADER_USER_TYPE, user_type));
		if(scope.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_SCOPE, scope.get()));
		if(clientId.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_CLIENT_ID, clientId.get()));
		if(tppName.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_TPP_NAME, tppName.get()));
		return request;
	}
	
	protected HttpRequest createPost(
		String url, String user_type, 
		Optional<String> scope, Optional<String> clientId, Optional<String> tppName
	) {
		HttpRequest request = HttpRequest.POST(url)
			.addHeader(RawHeader.create(HEADER_LANG, lang))
			.addHeader(RawHeader.create(HEADER_UUID, uuid))
			.addHeader(RawHeader.create(HEADER_USER_ID, user_id))
			.addHeader(RawHeader.create(HEADER_USER_CODE, user_code))
			.addHeader(RawHeader.create(HEADER_USER_TYPE, user_type));
		if(scope.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_SCOPE, scope.get()));
		if(clientId.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_CLIENT_ID, clientId.get()));
		if(tppName.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_TPP_NAME, tppName.get()));
		return request;
	}
	
	protected static AccountsModel createAccountsModel(String ... ibanList) {
		AccountsModel am = new AccountsModel();
		am.setAbi(abi);
		am.setIstituto3n(String.valueOf(istituto3n));
		am.setContratto("contratto");
		am.setNdg(user_code);
		am.setIbanList(new ArrayList<>());	
		
		if(ibanList!=null)
			for(String iban:ibanList) {
				IbanModel ibanModel = new IbanModel();
				ibanModel.setConto(1);
				ibanModel.setFiliale(2);
				ibanModel.setIban(iban);
				ibanModel.setServizio(servizio);
				ibanModel.setCurrency("EUR");
				am.getIbanList().add(ibanModel);
			}
		return am;
	}
}