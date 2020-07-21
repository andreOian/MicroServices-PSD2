package it.sotto.psd2.msa.massimali.helper;

import it.sotto.psd2.msa.CommonConstants;
import it.sotto.psd2.msa.model.AccountsModel;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Oian Andrea
 */
public abstract class AbstractMassimaliHelper {
	protected Logger log = LoggerFactory.getLogger(CommonConstants.LOGGER_APILOG);
	
	public abstract String inquiry(
		String uuid, String abi, String userId, String userType, String userCode, 
		Optional<String> scope, 
		AccountsModel accounts, String accountId, 
		String amountStr, String lang, String currency, 
		Optional<String> codiceFunzione, Optional<String> fnName, Optional<String> tppName, 
		String functionName
	);
	
	public abstract String update(
		String uuid, String abi, String userId, String userType, String userCode, 
		Optional<String> scope, AccountsModel accounts, 
		String accountId, String amountStr, String lang, String currency, 
		Optional<String> codiceFunzione, Optional<String> fnName, Optional<String> tppName,
		String functionName
	);
}
