package it.sotto.psd2.msa.massimali.helper;

import cb.ws.terzeparti.CbAggiornamentoMassimaliResponse;
import cb.ws.terzeparti.CbVerificaMassimaliResponse;
import it.sotto.psd2.msa.esb.ESBContrattoCorporate;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_FAILURE;
import static it.sotto.psd2.msa.massimali.Constants.RESPONSE_SUCCESS;
import it.sotto.psd2.msa.model.AccountsModel;
import it.sotto.psd2.msa.model.IbanModel;
import it.sotto.psd2.msa.util.Utils;
import java.util.Optional;

/**
 *
 * @author Oian Andrea
 */
public class CorporateHelper extends AbstractMassimaliHelper {
	
	private ESBContrattoCorporate esbContrattoCorporate = null;
	
	public CorporateHelper(ESBContrattoCorporate esbContrattoCorporate) {
		this.esbContrattoCorporate = esbContrattoCorporate;
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
        log.debug(
			"corporate inquiry abi[{}] userCode[{}] accountId[{}] amountStr[{}] currency[{}] codiceFunzione[{}]",
			abi, userCode, accountId, amountStr, currency, codiceFunzione
		);
        try {
            Optional<IbanModel> iban = extractFrom(accounts, accountId);
            if(iban.isPresent()) {				
				CbVerificaMassimaliResponse verificaMassimaliResponse = esbContrattoCorporate.verificaMassimali(
					abi, userCode, accounts.getCodiceAzienda(), codiceFunzione.orElse(null), accountId, 
					String.valueOf(iban.get().getServizio()), Utils.toBigDecimal(amountStr), currency
				);
				if(verificaMassimaliResponse.isEsito() && verificaMassimaliResponse.isVerificaSuperata()) {
					return RESPONSE_SUCCESS;
				}
            } else {
				log.error("error calling MassimaliCorporate.inquiry: account {} not found",accountId);
			}
        } catch (Exception ex) {
            log.error("error calling MassimaliCorporate.inquiry", ex);
        }
        return RESPONSE_FAILURE;
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
        log.debug(
			"corporate update abi[{}] userCode[{}] accountId[{}] amountStr[{}] currency[{}] codiceFunzione[{}]",
			abi, userCode, accountId, amountStr, currency, codiceFunzione
		);
        try {
            Optional<IbanModel> iban = extractFrom(accounts, accountId);
            if(iban.isPresent()) {
                CbAggiornamentoMassimaliResponse aggiornamentoMassimaliResponse = esbContrattoCorporate.aggiornamentoMassimali(
					abi, userCode, accounts.getCodiceAzienda(), codiceFunzione.orElse(null), accountId, 
					String.valueOf(iban.get().getServizio()), Utils.toBigDecimal(amountStr), currency
				);
				if(aggiornamentoMassimaliResponse.isEsito()) {
					return RESPONSE_SUCCESS;
				}
            } else {
				log.error("error calling MassimaliCorporate.update: account {} not found",accountId);
			}
        } catch (Exception ex) {
            log.error("error calling MassimaliCorporate.update", ex);
        }
        return RESPONSE_FAILURE;
	}
	
    private Optional<IbanModel> extractFrom(AccountsModel accounts, String accountId) {
        return accounts.getIbanList().stream().filter(ibanModel -> ibanModel.getIban().equalsIgnoreCase(accountId)).findFirst();
    }
}