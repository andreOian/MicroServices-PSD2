package it.sotto.psd2.msa.massimali;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import cb.ws.terzeparti.CbAggiornamentoMassimaliResponse;
import cb.ws.terzeparti.CbVerificaMassimaliResponse;
import it.sotto.psd2.msa.model.AccountsModel;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Test;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 *
 * @author Oian Andrea
 * 
 */
public class TestCorporateRoutes extends TestRoutes {

	private String amountStr = "1.25";
	private String user_type = "CORPORATE";
	private BigDecimal amount = new BigDecimal(amountStr);

	
	public TestCorporateRoutes() {
		super();
	}
	
	@Test 
	public void test_verifica() throws Exception {
		HttpRequest request1 = createGet(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/inquiry", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);

		CbVerificaMassimaliResponse response;

		response = new CbVerificaMassimaliResponse();
		response.setEsito(true);
		response.setVerificaSuperata(true);
		mock_ws_verifica(amount, "EUR", accounts, response);

		testRoute.run(request1).assertStatusCode(StatusCodes.OK);

		response = new CbVerificaMassimaliResponse();
		response.setEsito(true);
		response.setVerificaSuperata(false);
		mock_ws_verifica(amount, "EUR", accounts, response);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);

		response = new CbVerificaMassimaliResponse();
		response.setEsito(false);
		response.setVerificaSuperata(false);
		mock_ws_verifica(amount, "EUR", accounts, response);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);

		response = new CbVerificaMassimaliResponse();
		response.setEsito(false);
		response.setVerificaSuperata(true);
		mock_ws_verifica(amount, "EUR", accounts, response);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);

		HttpRequest request2 = createGet(
			addParameters("/psd2/v1/"+abi+"/accounts/123456789/massimali/inquiry", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);
		
		testRoute.run(request2).assertStatusCode(StatusCodes.OK);
	}
	
	@Test 
	public void test_verifica_exception() throws Exception {
		HttpRequest request1 = createGet(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/inquiry", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);

		mock_ws_verifica_exception(amount, uuid, accounts);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);
	}	

	@Test 
	public void testMassimali_update() throws Exception {		
		HttpRequest request1 = createPost(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/update", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);

		CbAggiornamentoMassimaliResponse response;

		response = new CbAggiornamentoMassimaliResponse();
		response.setEsito(true);
		mock_ws_update(amount, "EUR", accounts, response);

		testRoute.run(request1).assertStatusCode(StatusCodes.OK);

		response = new CbAggiornamentoMassimaliResponse();
		response.setEsito(false);
		mock_ws_update(amount, "EUR", accounts, response);

		testRoute.run(request1).assertStatusCode(StatusCodes.OK);

		HttpRequest request2 = createPost(
			addParameters("/psd2/v1/"+abi+"/accounts/123456789/massimali/update", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);

		testRoute.run(request2).assertStatusCode(StatusCodes.OK);
	}
	
	@Test 
	public void test_update_exception() throws Exception {
		HttpRequest request1 = createPost(
			addParameters("/psd2/v1/"+abi+"/accounts/"+iban1+"/massimali/update", amountStr, "EUR"), 
			user_type,
			Optional.empty(), Optional.empty(), Optional.empty()
		);

		mock_ws_update_exception(amount, uuid, accounts);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);
	}	

	
	private String addParameters(String url, String amountStr, String currency) {
		return 
			url+
			"?"+Constants.PARS_AMOUNT+"="+amountStr+
			"&"+Constants.PARS_CURRENCY+"="+currency+
			"&"+Constants.PARS_CODICE_FUNZIONE+"=prova";
	}
	
	private void mock_ws_verifica(BigDecimal amount, String currency, AccountsModel accounts, CbVerificaMassimaliResponse response) throws Exception {
		doReturn(response)
		.when(esbCorporate)
		.verificaMassimali(
			abi, user_code, accounts.getCodiceAzienda(), "prova", iban1, 
			String.valueOf(servizio), amount, currency
		);

		doReturn(response)
		.when(esbCorporate)
		.verificaMassimali(
			abi, user_code, accounts.getCodiceAzienda(), null, iban1, 
			String.valueOf(servizio), amount, currency
		);
	}

	private void mock_ws_verifica_exception(BigDecimal amount, String currency, AccountsModel accounts) throws Exception {
		doThrow(Exception.class)
		.when(esbCorporate)
		.verificaMassimali(
			abi, user_code, accounts.getCodiceAzienda(), "prova", iban1, 
			String.valueOf(servizio), amount, currency
		);

		doThrow(Exception.class)
		.when(esbCorporate)
		.verificaMassimali(
			abi, user_code, accounts.getCodiceAzienda(), null, iban1, 
			String.valueOf(servizio), amount, currency
		);
	}
	
	private void mock_ws_update(BigDecimal amount, String currency, AccountsModel accounts, CbAggiornamentoMassimaliResponse response) throws Exception {
		doReturn(response)
		.when(esbCorporate)
		.aggiornamentoMassimali(
			abi, user_code, accounts.getCodiceAzienda(), "prova", iban1, 
			String.valueOf(servizio), amount, currency
		);

		doReturn(response)
		.when(esbCorporate)
		.aggiornamentoMassimali(
			abi, user_code, accounts.getCodiceAzienda(), null, iban1, 
			String.valueOf(servizio), amount, currency
		);
	}

	private void mock_ws_update_exception(BigDecimal amount, String currency, AccountsModel accounts) throws Exception {
		doThrow(Exception.class)
		.when(esbCorporate)
		.aggiornamentoMassimali(
			abi, user_code, accounts.getCodiceAzienda(), "prova", iban1, 
			String.valueOf(servizio), amount, currency
		);

		doThrow(Exception.class)
		.when(esbCorporate)
		.aggiornamentoMassimali(
			abi, user_code, accounts.getCodiceAzienda(), null, iban1, 
			String.valueOf(servizio), amount, currency
		);
	}
}