package it.sotto.psd2.msa.massimali.helper;

import static it.sotto.psd2.msa.CommonConstants.ABI_BMED;
import it.sotto.psd2.msa.massimali.util.MassimaliDataAccess;
import it.sotto.psd2.msa.massimali.util.WSMassimali;
import it.sotto.psd2.msa.model.AccountsModel;
import java.math.BigDecimal;
import java.util.Optional;

/**
 *
 * @author Oian Andrea
 */
public class RetailHelper extends AbstractMassimaliHelper {

	private WSMassimali ws = null;
	private final MassimaliDataAccess db;
	
	public RetailHelper(WSMassimali ws, MassimaliDataAccess db) {
		this.ws = ws;
		this.db = db;
	}

	@Override
	public String inquiry(
		String uuid, String abi, String userId, String userType, String userCode, 
		Optional<String> scope, 
		AccountsModel accounts, String accountId, 
		String amountStr, String lang, String currency, 
		Optional<String> codiceFunzione, Optional<String> fnName, Optional<String> tppName, 
		String functionName
	) {
		if (abi.equals(ABI_BMED)) {
			return db.inquiry(uuid, abi, userCode, userType, currency, new BigDecimal(amountStr));
		} else {
			return ws.inquiry(uuid, abi, userType, lang, currency, new BigDecimal(amountStr), accounts);
		}
	}

	@Override
	public String update(
		String uuid, String abi, String userId, String userType, String userCode, 
		Optional<String> scope, 
		AccountsModel accounts, String accountId, 
		String amountStr, String lang, String currency, 
		Optional<String> codiceFunzione, Optional<String> fnName, Optional<String> tppName, 
		String functionName
	) {
		if (abi.equals(ABI_BMED)) {
			return db.update(uuid, abi, userCode, userType, currency, new BigDecimal(amountStr));
		} else {
			return ws.update(uuid, abi, userType, lang, currency, new BigDecimal(amountStr), accounts);
		}
	}
}
