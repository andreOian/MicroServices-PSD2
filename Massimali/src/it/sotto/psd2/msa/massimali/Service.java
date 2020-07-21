package it.sotto.psd2.msa.massimali;

import akka.actor.ActorSystem;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.server.PathMatcher0;
import akka.http.javadsl.server.PathMatcher2;
import static akka.http.javadsl.server.PathMatchers.segment;
import akka.http.javadsl.server.Route;
import com.typesafe.config.Config;
import it.sotto.psd2.msa.AbstractDirectives;
import it.sotto.psd2.msa.CommonConstants;
import static it.sotto.psd2.msa.CommonConstants.HEADER_CLIENT_ID;
import static it.sotto.psd2.msa.CommonConstants.HEADER_LANG;
import static it.sotto.psd2.msa.CommonConstants.HEADER_SCOPE;
import static it.sotto.psd2.msa.CommonConstants.HEADER_TPP_NAME;
import static it.sotto.psd2.msa.CommonConstants.HEADER_USER_CODE;
import static it.sotto.psd2.msa.CommonConstants.HEADER_USER_ID;
import static it.sotto.psd2.msa.CommonConstants.HEADER_USER_TYPE;
import static it.sotto.psd2.msa.CommonConstants.HEADER_UUID;
import it.sotto.psd2.msa.db.Database;
import it.sotto.psd2.msa.esb.ESBContrattoCorporate;
import it.sotto.psd2.msa.exception.ServiceExecutionExceptionBG;
import it.sotto.psd2.msa.exception.model.ErrorCodeBG;
import it.sotto.psd2.msa.helper.MSAccountsHelper;
import it.sotto.psd2.msa.helper.MSCruscottoNRTHelper;
import it.sotto.psd2.msa.massimali.helper.AbstractMassimaliHelper;
import it.sotto.psd2.msa.massimali.helper.CorporateHelper;
import it.sotto.psd2.msa.massimali.helper.RetailHelper;
import it.sotto.psd2.msa.massimali.util.MassimaliDataAccess;
import it.sotto.psd2.msa.massimali.util.WSMassimali;
import it.sotto.psd2.msa.model.AccountsModel;
import it.sotto.psd2.msa.model.cruscotto.KafkaMessageModel;
import it.sotto.psd2.msa.util.HttpUtils;
import static it.sotto.psd2.msa.util.LoggerUtils.initLogger;
import static it.sotto.psd2.msa.util.LoggerUtils.withMdc;
import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

/**
 *
 * @author Oian Andrea
 *
 */
public class Service extends AbstractDirectives {

	private static final String APP_NAME = "Massimali";

	private static final String EUR = "EUR";

	private static final Pattern ABI_MATCHER        = Pattern.compile("[0-9]{5}");
	private static final Pattern NDG_MATCHER        = Pattern.compile("[0-9]{1,16}"); //TODO dimensioni?
	private static final Pattern USERTYPE_MATCHER   = Pattern.compile("(RETAIL|CORPORATE)");
	private static final Pattern ACCOUNT_ID_MATCHER = Pattern.compile("[a-zA-Z0-9]+");

	private final MassimaliDataAccess db;
	
	protected AbstractMassimaliHelper retailHelper = null;
	protected AbstractMassimaliHelper corporateHelper = null;

	public Service(
		ActorSystem system, Config config, Uri serviceUri, 
		WSMassimali ws, ESBContrattoCorporate esbContrattoCorporate, 
		MassimaliDataAccess db
	) {
		super(system, config, serviceUri);
		this.db = db;
		this.retailHelper = new RetailHelper(ws, db);
		this.corporateHelper = new CorporateHelper(esbContrattoCorporate);
	}

	@Override
	public CompletionStage<ServerBinding> run() {
		if(ofNullable(db).isPresent() && db instanceof Database)
			((Database)db).connect();
		return super.run();
	}

	@Override
	protected boolean healthcheck() {
		return ofNullable(db).isPresent()?db.healthcheck():false;
	}

	@Override
	protected void onShutdown() {
		if(ofNullable(db).isPresent() && db instanceof Database)
			((Database)db).closeConnections();
	}

	// psd2/v1/{abi}/accounts/{account_id}/massimali
	private static final PathMatcher2<String,String> PATH_MASSIMALI =
		segment("psd2")
		.slash("v1")
		.slash(segment(ABI_MATCHER))
		.slash("accounts")
		.slash(segment(ACCOUNT_ID_MATCHER))
		.slash("massimali");

	// PATH_MASSIMALI/inquiry
	private static final PathMatcher0 PATH_INQUIRY =
		segment("inquiry");

	// PATH_MASSIMALI/update
	private static final PathMatcher0 PATH_UPDATE =
		segment("update");

	// massimali/v1/03062/{ndg}/{user_type}
	private static final PathMatcher2<String,String> PATH_MASSIMALI_BMED =
		segment("massimali")
		.slash("v1")
		.slash("03062")
		.slash(segment(NDG_MATCHER))
		.slash(segment(USERTYPE_MATCHER));


	@Override
	public Route createRoute() {
		return route(
			pathPrefix(PATH_MASSIMALI, (abi, accountId) ->
				headerValueByName(HEADER_LANG, lang ->
				headerValueByName(HEADER_UUID, uuid ->
				headerValueByName(HEADER_USER_ID, userId ->
				headerValueByName(HEADER_USER_TYPE, userType ->
				headerValueByName(HEADER_USER_CODE, userCode ->
				optionalHeaderValueByName(HEADER_SCOPE, scope ->
				optionalHeaderValueByName(HEADER_CLIENT_ID, clientId ->
				optionalHeaderValueByName(HEADER_TPP_NAME, tppName ->
				extractRequest(request ->
				route(
					path(PATH_INQUIRY, () -> 
						get(() -> 
							parameter(Constants.PARS_AMOUNT, amountStr -> 
							parameter(Constants.PARS_CURRENCY, currency -> 
							parameterOptional(Constants.PARS_CODICE_FUNZIONE, codiceFunzione ->
							parameterOptional(Constants.PARS_FN_NAME, fnName ->
							{
								initLogger(uuid, APP_NAME, "inquiry");
								Optional newScope=(scope.isPresent() && scope.get().isEmpty())?Optional.empty():scope; //gabola
								log.info(
									"(START) abi[{}] lang[{}] userId[{}] userType[{}] userCode[{}] amount[{}] currency[{}] scope[{}] funzione[{}] clientId[{}] fnName[{}]",
									abi, lang, userId, userType, userCode, amountStr, currency, newScope, codiceFunzione, clientId, fnName
								);
								return execute(uuid, abi, userId, userType, userCode, newScope, accountId, amountStr, lang, currency, codiceFunzione, fnName, tppName, true, request);
							}))))
						)
					),
					path(PATH_UPDATE, () ->
						post(() ->
							parameter(Constants.PARS_AMOUNT, amountStr -> 
							parameter(Constants.PARS_CURRENCY, currency -> 
							parameterOptional(Constants.PARS_CODICE_FUNZIONE, codiceFunzione ->
							parameterOptional(Constants.PARS_FN_NAME, fnName ->
							{
								initLogger(uuid, APP_NAME, "update");
								Optional newScope=(scope.isPresent() && scope.get().isEmpty())?Optional.empty():scope; //gabola
								log.info(
									"(START) abi[{}] lang[{}] userId[{}] userType[{}] userCode[{}] amount[{}] currency[{}] scope[{}] funzione[{}] clientId[{}] fnName[{}]",
									abi, lang, userId, userType, userCode, amountStr, currency, newScope, codiceFunzione, clientId, fnName
								);
								return execute(uuid, abi, userId, userType, userCode, newScope, accountId, amountStr, lang, currency, codiceFunzione, fnName, tppName, false, request);
							}))))
						)
					)
				))))))))))
			),
			path(PATH_MASSIMALI_BMED, (ndg, userType) ->
				headerValueByName(HEADER_UUID, uuid ->
				optionalHeaderValueByName(HEADER_TPP_NAME, tppName ->
				route(
					get(() -> 
					{
						initLogger(uuid, APP_NAME, "massimali-bmed-get");
						log.info("(START) ndg[{}] userType[{}]", ndg, userType);
						return sendResponse(uuid, CompletableFuture.supplyAsync(() -> {
							return db.getMassimali(EUR, CommonConstants.ABI_BMED, ndg, userType);
						}));
					}),
					post(() ->
						parameter(Constants.PARS_MAX_DAILY, maxDailyStr ->
						parameter(Constants.PARS_MAX_MONTHLY, maxMonthlyStr ->
						{
							initLogger(uuid, APP_NAME, "massimali-bmed-post");
							log.info(
								"(Sndg[{}] userType[{}] maxDaily[{}] maxMonthly[{}]",
								ndg, userType, maxDailyStr, maxMonthlyStr
							);
							return sendResponse(uuid, CompletableFuture.supplyAsync(() -> {
								return db.updateMassimali(uuid, CommonConstants.ABI_BMED, ndg, userType, toBigDecimal(maxDailyStr), toBigDecimal(maxMonthlyStr));
							}));
						}))
					)
				)))
			)
		);
	}

	@Override
	protected String getServiceName() {
		return APP_NAME;
	}
	
	public AccountsModel getAccounts(
		boolean berlinGroup, 
		String uuid, String abi, String userId, String userType, String userCode, String accountId, 
		Optional<String> scope,
		HttpRequest request
	) {
		return MSAccountsHelper.getAccounts(
			berlinGroup, system, materializer, getServiceName(), 
			uuid, abi, userId, of(userType), userCode, accountId, 
			scope, 
			Optional.empty(), 
			HttpUtils.getHeaders(request)
		);
	}

	protected void logNRT(String uuid, KafkaMessageModel message) {
		MSCruscottoNRTHelper.writeLOG(system, materializer, uuid, getServiceName(), message);
	}

	private BigDecimal toBigDecimal(String s) {
		BigDecimal n = null;
		try {
			s=ofNullable(s).map(str -> "000"+str).orElse("000");
			n = new BigDecimal(s.substring(0,s.length()-2)+"."+s.substring(s.length()-2));
		} catch(Exception e) {  }
		return n;
	}
	
	private Route execute(
		String uuid, String abi, String userId, String userType, String userCode, 
		Optional<String> scope, 
		String accountId, String amountStr, String lang, String currency, 
		Optional<String> codiceFunzione, Optional<String> fnName, Optional<String> tppName, 
		boolean isInquiry, 
		HttpRequest request
	) {
		return sendResponse(uuid, CompletableFuture.supplyAsync(withMdc(() -> {
			testParameters(currency, amountStr);
			AccountsModel accounts=getAccounts(false, uuid, abi, userId, userType, userCode, accountId, scope, request);
			
			String ofn=fnName.filter(n->!n.trim().isEmpty()).map(s->" ("+s+")").orElse("");
			
			if(userType.equalsIgnoreCase("Corporate")) {
				if(isInquiry) {
					String esito=
						corporateHelper.inquiry(uuid, abi, userId, userType, userCode, scope, accounts, accountId, amountStr, lang, currency, codiceFunzione, fnName, tppName, APP_NAME);
					logNRT(uuid, createKafkaMessage(
						uuid, null, scope, "Verifica massimali"+ofn,
						abi, userId, userType, userCode, codiceFunzione, accountId, amountStr, esito, tppName
					));
					return esito;
				} else
					return 
						corporateHelper.update(uuid, abi, userId, userType, userCode, scope, accounts, accountId, amountStr, lang, currency, codiceFunzione, fnName, tppName, APP_NAME);
			} else {
				if(isInquiry) {
					String esito=
						retailHelper.inquiry(uuid, abi, userId, userType, userCode, scope, accounts, accountId, amountStr, lang, currency, codiceFunzione, fnName, tppName, APP_NAME);
					logNRT(uuid, createKafkaMessage(
						uuid, null, scope, "Verifica massimali"+ofn,
						abi, userId, userType, userCode, codiceFunzione, accountId, amountStr, esito, tppName
					));
					return esito;
				} else {
					return 
						retailHelper.update(uuid, abi, userId, userType, userCode, scope, accounts, accountId, amountStr, lang, currency, codiceFunzione, fnName, tppName, APP_NAME);
				}
			}
		})));
	}
	
	private void testParameters(String currency, String amountStr) {
		String cur=ofNullable(currency).map(c -> c.toUpperCase()).orElse(EUR);
		if(!EUR.equals(cur))
			throw new ServiceExecutionExceptionBG(ErrorCodeBG.PARAMETER_NOT_SUPPORTED, String.format("invalid currency %s", currency));
		boolean isValid = false;
		try {
			BigDecimal amount = new BigDecimal(amountStr);
			if(amount.compareTo(ZERO)>0)
				isValid = true;
		} catch(Exception e) { }
		if(!isValid)
			throw new ServiceExecutionExceptionBG(ErrorCodeBG.FORMAT_ERROR, String.format("invalid amount %s", amountStr));
	}
	
	private KafkaMessageModel createKafkaMessage(
			String uuid, String client_id, Optional<String> scope, String nomeOperazione, 
			String abi, String userId, String userType, String userCode, 
			Optional<String> codiceFunzione, String accountId, String amount, String esito,
			Optional<String> tppName
	) {
		Map<String,Object> corpo = new HashMap<>();
		corpo.put("account_id", accountId);
		corpo.put("amount", amount);
		if(codiceFunzione.isPresent())
			corpo.put("functionCode", codiceFunzione.get());
		corpo.put("result", esito);

		KafkaMessageModel message = new KafkaMessageModel();
		message.buildHeader(abi, userId, userType, userCode, "ruolo");
		message.setCodApplicazione("PSD2");
		message.setSorgente("Massimali");
		message.setIdOperazione(uuid);
		message.setNomeOperazione(nomeOperazione);
		message.setTipoTPP(scope.orElse(""));
		message.setClientIdTPP(client_id);
		message.setNomeTPP(tppName.orElse(null));
		message.setTimestamp(new Date());
		message.setStato(esito);
		message.setCorpo(corpo);
		return message;
	}
}