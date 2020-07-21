package it.sotto.psd2.msa.massimali;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.RawHeader;
import static it.sotto.psd2.msa.CommonConstants.ABI_BMED;
import static it.sotto.psd2.msa.CommonConstants.HEADER_TPP_NAME;
import static it.sotto.psd2.msa.CommonConstants.HEADER_UUID;
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
public class TestRetailBMEDRoutes extends TestRoutes {

	private String amountStr = "1.25";
	private String user_type = "RETAIL";
	private BigDecimal amount = new BigDecimal(amountStr);

	
	public TestRetailBMEDRoutes() {
		super();
	}
	
	@Test 
	public void testMassimali_inquiry() throws Exception {
		accounts.setAbi(ABI_BMED);
//		mock_ws_inquiry(amount, accounts, RESPONSE_SUCCESS);

		HttpRequest request1 = createGet(
			"/massimali/v1/"+ABI_BMED+"/"+user_code+"/"+user_type,
			Optional.empty()
		);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);
	}

	@Test 
	public void testMassimali_update() throws Exception {
		accounts.setAbi(ABI_BMED);
//		mock_ws_inquiry(amount, accounts, RESPONSE_SUCCESS);

		HttpRequest request1 = createPost(
			"/massimali/v1/"+ABI_BMED+"/"+user_code+"/"+user_type+
			"?"+Constants.PARS_MAX_DAILY+"=25000"+ //-->250.00
			"&"+Constants.PARS_MAX_MONTHLY+"=150000", //-->1500.00
			Optional.empty()
		);
		
		testRoute.run(request1).assertStatusCode(StatusCodes.OK);
	}
	
	protected HttpRequest createGet(String url, Optional<String> tppName) {
		HttpRequest request = HttpRequest.GET(url)
			.addHeader(RawHeader.create(HEADER_UUID, uuid));
		if(tppName.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_TPP_NAME, tppName.get()));
		return request;
	}
	
	protected HttpRequest createPost(String url, Optional<String> tppName) {
		HttpRequest request = HttpRequest.POST(url)
			.addHeader(RawHeader.create(HEADER_UUID, uuid));
		if(tppName.isPresent())
			request=request.addHeader(RawHeader.create(HEADER_TPP_NAME, tppName.get()));
		return request;
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