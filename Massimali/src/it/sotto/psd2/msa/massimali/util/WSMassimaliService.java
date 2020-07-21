package it.sotto.psd2.msa.massimali.util;

import it.cedacri.api.mf_te_servizitelematici.servizitelematici.ServiziTelematiciBindingQSService;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.ServiziTelematiciPort;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.limitioperativiaggiorna.schemas.in.LimitiOperativiAggiornaRichiesta;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.limitioperativiinquiry.schemas.in.LimitiOperativiInquiryRichiesta;
import it.cedacri.api.mf_te_servizitelematici.servizitelematici.limitioperativiinquiry.schemas.out.LimitiOperativiInquiryRisposta;
import static it.sotto.psd2.msa.Config.getValue;
import it.sotto.psd2.msa.esb.AbstractSOAPInvoker;
import it.sotto.psd2.msa.exception.ServiceExecutionExceptionOld;
import it.sotto.psd2.msa.massimali.Constants;
import static it.sotto.psd2.msa.massimali.Constants.ENV_WEBSERVICE_URL;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_FAILURE;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_SUCCESS;
import it.sotto.psd2.msa.massimali.model.MassimaliModel;
import it.sotto.psd2.msa.model.AccountsModel;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import static java.util.Optional.ofNullable;
import javax.xml.ws.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSMassimaliService extends AbstractSOAPInvoker<ServiziTelematiciPort> implements WSMassimali {

	private static final String LOG_START_METHOD = "[{}] Massimali::{} abi [{}] userType [{}] lang [{}] currency [{}] amount [{}]";
	protected Logger log = LoggerFactory.getLogger(getClass());


	@Override
	protected String getEndpoint(String abi) {
		return getValue(ENV_WEBSERVICE_URL);
	}

	@Override
	protected Service createClient(String endpoint) throws MalformedURLException {
		return new ServiziTelematiciBindingQSService(
			ClassLoader.getSystemClassLoader().getResource("wsdl/servizitelematici/ServiziTelematici.wsdl")
		);
	}

	@Override
	protected ServiziTelematiciPort createPort(Service service) {
		return ((ServiziTelematiciBindingQSService)service).getServiziTelematiciBindingQSPort();
	}

	@Override
	public String inquiry(String uuid, String abi, String userType, String lang, String currency, BigDecimal amount, AccountsModel accounts) {
		log.debug(LOG_START_METHOD, uuid, "inquiry", abi, userType, lang, currency, amount.toPlainString());
		MassimaliModel model = null;
		try {
			model = limitiOperativiInquiry(uuid, lang, amount, accounts,abi);
		} catch (Exception e) {
			log.error(String.format("[%s] error calling ServiziTelematici.limitiOperativiInquiry", uuid), e);
			throw new ServiceExecutionExceptionOld();
		}
		
		return CoverageUtils.isPaymentCovered(model, amount);
	}

	@Override
	public String update(String uuid, String abi, String userType, String lang, String currency, BigDecimal amount, AccountsModel accounts) {
		log.debug(LOG_START_METHOD, uuid, "update", abi, userType, lang, currency, amount.toPlainString());
		try {
			limitiOperativiAggiorna(uuid, lang, amount, accounts,abi);
		} catch (Exception e) {
			log.error("["+uuid+"] error calling ServiziTelematici.limitiOperativiSelfServiceAggiorna", e);
			return RESPONSE_FAILURE;
		}
		return RESPONSE_SUCCESS;
	}

	@Override
	public boolean healthcheck() {
		return true;
	}

	public MassimaliModel limitiOperativiInquiry(String uuid, String lang, BigDecimal amount, AccountsModel accounts,String abi) throws Exception {
		LimitiOperativiInquiryRichiesta req = createLimitiOperativiInquiryRichiesta(uuid, amount, lang, accounts);
		LimitiOperativiInquiryRisposta res = getESBService(abi).limitiOperativiInquiry(req);

		MassimaliModel model = new MassimaliModel();
		model.setMaxGiornaliero(res.getMaxGiornaliero());
		model.setMaxMensile(res.getMaxMensile());
		model.setUtilizzatoGiornalieroOdierno(res.getUtilizzatoGiornalieroOdierno());
		model.setUtilizzatoMensileCorrente(res.getUtilizzatoMensileCorrente());
		return model;
	}
	
	public void limitiOperativiAggiorna(String uuid, String lang, BigDecimal amount, AccountsModel accounts,String abi) throws Exception {
		getESBService(abi).limitiOperativiAggiorna(createLimitiOperativiAggiornaRichiesta(uuid, amount, lang, accounts));
	}

	/**
	 * @param uuid
	 * @param amount
	 * @param lang
	 * @return
	 */
	private LimitiOperativiInquiryRichiesta createLimitiOperativiInquiryRichiesta(String uuid, BigDecimal amount, String lang, AccountsModel accounts) {
		LimitiOperativiInquiryRichiesta r = new LimitiOperativiInquiryRichiesta();
		r.setIdApplicazioneChiamante(Constants.ID_APPLICAZIONE_CHIAMANTE);
		r.setIdSessioneChiamante(uuid);
		r.setCodiceIstituto3N(ofNullable(accounts.getIstituto3n()).map(Integer::parseInt).orElse(0));
		r.setIdOperazione(uuid);
		r.setUtenzaCanale(Constants.UTENZA_CANALE);
		r.setNDGUtente(Long.parseLong(accounts.getNdg()));
		r.setNumeroContratto(accounts.getContratto());
		r.setImporto(amount);
		r.setLingua(lang);
		return r;
	}

	/**
	 * @param uuid
	 * @param amount
	 * @param lang
	 * @param accounts
	 * @return
	 * @throws Exception 
	 */
	private LimitiOperativiAggiornaRichiesta createLimitiOperativiAggiornaRichiesta(String uuid, BigDecimal amount, String lang, AccountsModel accounts) throws Exception {
		LimitiOperativiAggiornaRichiesta r = new LimitiOperativiAggiornaRichiesta();
		r.setIdApplicazioneChiamante(Constants.ID_APPLICAZIONE_CHIAMANTE);
		r.setIdSessioneChiamante(uuid);
		r.setCodiceIstituto3N(ofNullable(accounts.getIstituto3n()).map(Integer::parseInt).orElse(0));
		r.setIdOperazione(uuid);
		r.setUtenzaCanale(Constants.UTENZA_CANALE);
		r.setNDGUtente(Long.parseLong(accounts.getNdg()));
		r.setNumeroContratto(accounts.getContratto());
		r.setImporto(amount);
		r.setLingua(lang);
		return r;
	}
}