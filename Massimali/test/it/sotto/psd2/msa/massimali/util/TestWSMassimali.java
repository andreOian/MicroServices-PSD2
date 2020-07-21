package it.sotto.psd2.msa.massimali.util;

import akka.http.javadsl.testkit.JUnitRouteTest;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.ServiziTelematiciBindingQSService;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.ServiziTelematiciPort;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.limitioperativiaggiorna.schemas.out.LimitiOperativiAggiornaRisposta;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.limitioperativiinquiry.schemas.out.LimitiOperativiInquiryRisposta;
import it.sotto.psd2.msa.Config;
import it.sotto.psd2.msa.exception.ServiceExecutionExceptionOld;
import it.sotto.psd2.msa.massimali.Constants;
import it.sotto.psd2.msa.massimali.model.MassimaliModel;
import it.sotto.psd2.msa.model.AccountsModel;

import java.math.BigDecimal;
import java.net.MalformedURLException;

import org.apache.log4j.PropertyConfigurator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.xml.ws.Service;

/**
 * @author Oian Andrea
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Config.class)
public class TestWSMassimali extends JUnitRouteTest {

    private WSMassimaliService esbMassimali = new WSMassimaliService();
    private static final String ENDPOINT = "localhost:1234";

    private final ServiziTelematiciPort portMock = mock(ServiziTelematiciPort.class);

    private class WSMassimaliServiceMock extends WSMassimaliService {
        @Override
        protected ServiziTelematiciPort getESBService(String abi) throws MalformedURLException {
            return portMock;
        }
    }

    private final WSMassimaliServiceMock clientMock = new WSMassimaliServiceMock();

    public TestWSMassimali() {
        PropertyConfigurator.configure("log4j.properties");
        PowerMockito.mockStatic(Config.class);
        PowerMockito.when(Config.getValue(Constants.ENV_WEBSERVICE_URL)).thenReturn(ENDPOINT);
    }


    @Test
    public void testEndpoint() {
        String endpoint = esbMassimali.getEndpoint("00000");
        assertEquals(ENDPOINT, endpoint);

        endpoint = esbMassimali.getEndpoint("11111");
        assertEquals(ENDPOINT, endpoint);
    }

    @Test
    public void testClientCreation() {
        Object client = null;
        try {
            client = esbMassimali.createClient(ENDPOINT);
        } catch (Exception ex) {
        }

        assertNotNull(client);
        assertTrue(client instanceof ServiziTelematiciBindingQSService);
    }

    @Test
    public void testPortCreation() {
        Object port = null;
		ServiziTelematiciBindingQSService portServiceMock = new ServiziTelematiciBindingQSService();
        try {
            port = esbMassimali.createPort(portServiceMock);
        } catch (Exception ex) {
        }

		assertNotNull(port);
        assertTrue(port instanceof ServiziTelematiciPort);
    }

    @Test
    public void testLimitiOperativiInquiry() throws Exception {
        createLimitiInquiryMock();

        AccountsModel accounts = createAccountsModel("0", "123", "111111");

        String response = null;

        response = clientMock.inquiry("uuid", "00000", "RETAIL", "it", "EUR", new BigDecimal(100), accounts);

        assertNotNull(response);
        assertEquals(Constants.RESPONSE_SUCCESS, response);

        response = clientMock.inquiry("uuid", "00000", "RETAIL", "it", "EUR", new BigDecimal(1000), accounts);

        assertNotNull(response);
        assertEquals(Constants.RESPONSE_FAILURE, response);
    }

    @Test(expected = ServiceExecutionExceptionOld.class)
    public void testLimitiOperativiInquiry_exceptionOld() throws Exception {
        createLimitiInquiryMock();

        clientMock.inquiry(
                "uuid", "00000", "RETAIL", "it", "EUR", new BigDecimal(100),
                createAccountsModel("a", "123", "111111")
        );
    }

    @Test(expected = Exception.class)
    public void testPaymentCovered_exception() throws Exception {
        BigDecimal amount = new BigDecimal(100);
        CoverageUtils.isPaymentCovered(null, amount);
    }

    private void createLimitiInquiryMock() throws Exception {
        LimitiOperativiInquiryRisposta fakeResponse = new LimitiOperativiInquiryRisposta();
        fakeResponse.setMaxGiornaliero(new BigDecimal(250));
        fakeResponse.setMaxMensile(new BigDecimal(1500));
        fakeResponse.setUtilizzatoGiornalieroOdierno(new BigDecimal(50));
        fakeResponse.setUtilizzatoMensileCorrente(new BigDecimal(300));

        when(portMock.limitiOperativiInquiry(any())).thenReturn(fakeResponse);
    }

    @Test
    public void testLimitiOperativiUpdate() throws Exception {
        createLimitiUpdateMock(false);

        String response = clientMock.update(
                "uuid", "00000", "RETAIL", "it", "EUR", new BigDecimal(100),
                createAccountsModel("0", "123", "111111")
        );

        assertNotNull(response);
        assertEquals(Constants.RESPONSE_SUCCESS, response);
    }

    @Test
    public void testLimitiOperativiUpdate_exception() throws Exception {
        createLimitiUpdateMock(true);

        String response = clientMock.update(
                "uuid", "00000", "RETAIL", "it", "EUR", new BigDecimal(100),
                createAccountsModel("0", "123", "111111")
        );

        assertNotNull(response);
        assertEquals(Constants.RESPONSE_FAILURE, response);
    }

    @Test
    public void testHealthCheck() throws Exception {
        String healtcheck = String.valueOf(clientMock.healthcheck());
        assertTrue(healtcheck, true);
    }

    private void createLimitiUpdateMock(boolean exception) throws Exception {
        if (exception) {
            when(portMock.limitiOperativiAggiorna(any())).thenThrow(Exception.class);
        } else {
            when(portMock.limitiOperativiAggiorna(any())).thenReturn(new LimitiOperativiAggiornaRisposta());
        }
    }

    private AccountsModel createAccountsModel(String istituto3n, String ndg, String contratto) {
        AccountsModel accounts = new AccountsModel();
        accounts.setIstituto3n(istituto3n);
        accounts.setNdg(ndg);
        accounts.setContratto(contratto);
        return accounts;
    }
}