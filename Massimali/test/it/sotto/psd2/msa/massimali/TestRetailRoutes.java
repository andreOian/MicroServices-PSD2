package it.sotto.psd2.msa.massimali;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import static it.sotto.psd2.msa.CommonConstants.ABI_BMED;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_SUCCESS;
import it.sotto.psd2.msa.model.AccountsModel;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Test;
import static org.mockito.Mockito.doReturn;

/**
 *
 * @author Oian Andrea
 * 
 */
public class TestRetailRoutes extends TestRoutes {

	private String amountStr = "1.25";
	private String user_type = "RETAIL";
	private BigDecimal amount = new BigDecimal(amountStr);

	
	public TestRetailRoutes() {
		super();
	}
	
	@Test 
	public void testMassimali_inquiry() throws Exception {
		accounts.setAbi(abi);
		mock_ws_inquiry(amount, accounts, RESPONSE_SUCCESS);

		HttpRequest request1 = createGet(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/inquiry", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);

		HttpRequest request2 = createGet(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/inquiry", amountStr, "USD"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		
		testRoute.run(request2).assertStatusCode(StatusCodes.BAD_REQUEST);

		HttpRequest request3 = createGet(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/inquiry", "pippo", "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		
		testRoute.run(request3).assertStatusCode(StatusCodes.BAD_REQUEST);
	}

	@Test 
	public void testMassimaliBMED_inquiry() throws Exception {
		accounts.setAbi(ABI_BMED);
		mock_ws_inquiry(amount, accounts, RESPONSE_SUCCESS);

		HttpRequest request1 = createGet(
			addParameters("/psd2/v1/"+ABI_BMED+"/accounts/"+iban1+"/massimali/inquiry", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);
	}

	@Test 
	public void testMassimali_update() throws Exception {
		accounts.setAbi(abi);
		mock_ws_update(amount, accounts, RESPONSE_SUCCESS);

		HttpRequest request1 = createPost(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/update", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);
	}

	@Test 
	public void testMassimaliBMED_update() throws Exception {
		accounts.setAbi(ABI_BMED);
		mock_ws_update(amount, accounts, RESPONSE_SUCCESS);

		HttpRequest request1 = createPost(
			addParameters("/psd2/v1/"+ABI_BMED+"/accounts/"+iban1+"/massimali/update", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);
	}
	
	private String addParameters(String url, String amountStr, String currency) {
		return 
			url+
			"?"+Constants.PARS_AMOUNT+"="+amountStr+
			"&"+Constants.PARS_CURRENCY+"="+currency+
			"&"+Constants.PARS_CODICE_FUNZIONE+"=prova";
	}
	
	private void mock_ws_inquiry(BigDecimal amount, AccountsModel accounts, String esito) throws Exception {
		doReturn(esito)
		.when(ws)
		.inquiry(uuid, abi, user_type, lang, "EUR", amount, accounts);
	}
	
	private void mock_ws_update(BigDecimal amount, AccountsModel accounts, String esito) throws Exception {
		doReturn(esito)
		.when(ws)
		.update(uuid, abi, user_type, lang, "EUR", amount, accounts);
	}
}